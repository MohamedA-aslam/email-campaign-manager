package com.campaign.service;

import com.campaign.model.SmtpProfile;
import com.campaign.model.SmtpProfile.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SmtpProfileService smtpProfileService;
    private final AwsSesService awsSesService;

    public String sendEmail(String to, String subject, String content) {

        SmtpProfile activeProfile = smtpProfileService.getActiveProfile();

        if (activeProfile == null) {
            log.warn("No active profile. Email not sent to {}", to);
            return "No active email profile configured. " +
                    "Please go to Profile page and activate one.";
        }

        if (activeProfile.getProvider() == EmailProvider.AMAZON_SES) {
            log.info("Sending via Amazon SES from {}", activeProfile.getEmail());
            return awsSesService.sendEmail(
                    activeProfile.getAwsAccessKey(),
                    activeProfile.getAwsSecretKey(),
                    activeProfile.getAwsRegion(),
                    activeProfile.getEmail(),
                    activeProfile.getDisplayName(),
                    to,
                    subject,
                    content
            );
        }

        // SMTP — use JavaMailSender built dynamically from profile
        log.info("Sending via JavaMailSender SMTP from {}", activeProfile.getEmail());
        return sendViaJavaMail(activeProfile, to, subject, content);
    }

    private String sendViaJavaMail(SmtpProfile profile,
                                   String to,
                                   String subject,
                                   String content) {
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(profile.getSmtpHost());
            mailSender.setPort(profile.getSmtpPort());
            mailSender.setUsername(profile.getEmail());
            mailSender.setPassword(profile.getAppPassword());

            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(profile.getDisplayName() + " <" + profile.getEmail() + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Email sent to {} via JavaMailSender from {}",
                    to, profile.getEmail());
            return null; // success

        } catch (MailException e) {
            log.error("JavaMailSender failed for {}: {}", to, e.getMessage());
            return e.getMessage();
        }
    }
}