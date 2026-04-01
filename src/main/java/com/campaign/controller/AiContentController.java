package com.campaign.controller;

import com.campaign.dto.GenerateContentRequest;
import com.campaign.dto.GenerateContentResponse;
import com.campaign.service.AnthropicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AiContentController {

    private final AnthropicService anthropicService;

    @PostMapping("/campaigns/generate-content")
    public ResponseEntity<GenerateContentResponse> generateContent(
            @RequestBody GenerateContentRequest request) {

        if (request.subject() == null || request.subject().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new GenerateContentResponse("Subject line cannot be empty."));
        }

        try {
            String content = anthropicService.generateEmailContent(request.subject());
            return ResponseEntity.ok(new GenerateContentResponse(content));
        } catch (Exception e) {
            log.error("Failed to generate email content", e);
            return ResponseEntity.internalServerError()
                    .body(new GenerateContentResponse("Failed to generate content. Please try again."));
        }
    }
}
