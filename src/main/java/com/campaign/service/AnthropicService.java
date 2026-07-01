package com.campaign.service;

import com.campaign.dto.GenerateContentResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnthropicService {

    @Value("${anthropic.api.key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Low-level: single call to Claude API, returns content[0].text
    // -------------------------------------------------------------------------
    private String callClaude(String userMessage) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return String.valueOf(ResponseEntity.status(503)
                    .body(new GenerateContentResponse("AI generation not configured on this server.")));
        }
        String body = objectMapper.writeValueAsString(Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 1024,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Anthropic API returned " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("content").get(0).path("text").asText();
    }

    // -------------------------------------------------------------------------
    // Email content generation (used by the "✨ Generate with AI" button)
    // -------------------------------------------------------------------------
    public String generateEmailContent(String subject) throws Exception {
        String prompt = "Write a professional marketing email body for a campaign with the subject line: '"
                + subject
                + "'. The email should be engaging, concise, 3-4 short paragraphs, include a call to action, "
                + "and end with a sign-off. Return only the email body text, no subject line, no extra explanation.";
        return callClaude(prompt);
    }

    // -------------------------------------------------------------------------
    // Campaign analysis insights (used after campaign execution)
    // Returns 3 sections: segmentInsights, failureBreakdown, suggestions
    // -------------------------------------------------------------------------
    public record InsightsResult(String segmentInsights, String failureBreakdown, String suggestions) {}

    public InsightsResult generateCampaignInsights(String campaignName, String subject,
                                                   long total, long sent, long failed,
                                                   List<String> failureReasons) {
        double deliveryRate = total > 0 ? Math.round(sent * 1000.0 / total) / 10.0 : 0.0;
        double failureRate  = total > 0 ? Math.round(failed * 1000.0 / total) / 10.0 : 0.0;

        String reasonsSummary = failureReasons.isEmpty()
                ? "No failure reasons captured."
                : String.join("; ", failureReasons.stream().limit(20).toList());

        String prompt = """
                You are an email campaign analytics expert. A campaign just finished with these real results:

                Campaign : "%s"
                Subject  : "%s"
                Total Recipients : %d
                Delivered        : %d (%.1f%%)
                Failed           : %d (%.1f%%)
                Failure reasons  : %s

                Return ONLY a valid JSON object — no markdown fences, no extra text — with exactly these 3 keys:
                {
                  "segmentInsights": "2-3 bullet points (use • ) about recipient segment performance based on typical email-domain patterns inferred from the failure data",
                  "failureBreakdown": "2-3 bullet points (use • ) categorising the likely failure causes (e.g. invalid emails, blocked domains, SMTP errors) based on the failure reasons above",
                  "suggestions": "2-3 bullet points (use • ) of concrete, actionable recommendations to improve the next campaign"
                }
                Each value must be a single string. Use \\n to separate bullet points within a string.
                """.formatted(campaignName, subject, total, sent, deliveryRate, failed, failureRate, reasonsSummary);

        try {
            String raw = callClaude(prompt).strip();
            // Strip markdown code fences if the model adds them anyway
            if (raw.startsWith("```")) {
                raw = raw.replaceAll("(?s)```[a-z]*\\s*|\\s*```", "").strip();
            }
            JsonNode json = objectMapper.readTree(raw);
            return new InsightsResult(
                    json.path("segmentInsights").asText("Segment insights unavailable."),
                    json.path("failureBreakdown").asText("Failure breakdown unavailable."),
                    json.path("suggestions").asText("Suggestions unavailable.")
            );
        } catch (Exception e) {
            log.error("Failed to generate campaign insights from Anthropic API", e);
            return new InsightsResult(
                    "Segment insights could not be generated.",
                    "Failure breakdown could not be generated.",
                    "Suggestions could not be generated."
            );
        }
    }
}
