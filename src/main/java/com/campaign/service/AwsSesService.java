package com.campaign.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Slf4j
public class AwsSesService {

    /**
     * Sends email via Amazon SES using credentials from the active profile.
     * Returns null on success, error message on failure.
     */
    public String sendEmail(String accessKey,
                            String secretKey,
                            String region,
                            String fromEmail,
                            String fromName,
                            String to,
                            String subject,
                            String content) {

        if (accessKey == null || accessKey.isBlank()) {
            return "AWS Access Key is missing. Please update your SES profile.";
        }
        if (secretKey == null || secretKey.isBlank()) {
            return "AWS Secret Key is missing. Please update your SES profile.";
        }
        if (region == null || region.isBlank()) {
            return "AWS Region is missing. Please update your SES profile.";
        }

        try {
            SesClient sesClient = SesClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();

            String fromAddress = (fromName != null && !fromName.isBlank())
                    ? fromName + " <" + fromEmail + ">"
                    : fromEmail;

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromAddress)
                    .destination(Destination.builder()
                            .toAddresses(to)
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data(subject)
                                    .charset("UTF-8")
                                    .build())
                            .body(Body.builder()
                                    .text(Content.builder()
                                            .data(content)
                                            .charset("UTF-8")
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Email sent via SES to {} — MessageId: {}",
                    to, response.messageId());
            sesClient.close();
            return null; // success

        } catch (SesException e) {
            log.error("SES error sending to {}: {}",
                    to, e.awsErrorDetails().errorMessage());
            return "SES Error: " + e.awsErrorDetails().errorMessage();
        } catch (Exception e) {
            log.error("SES send failed for {}: {}", to, e.getMessage());
            return e.getMessage();
        }
    }
}