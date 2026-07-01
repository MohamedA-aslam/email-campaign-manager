package com.campaign.service;

import com.campaign.model.SmtpProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SmtpProfileService smtpProfileService;

    public String sendEmail(String to, String subject, String content) {
        // Check for active profile — no fallback, error if missing
        SmtpProfile activeProfile = smtpProfileService.getActiveProfile();

        if (activeProfile == null) {
            log.warn("No active SMTP profile found. Email not sent to {}", to);
            return "No active SMTP profile configured. Please go to Profile page and activate one.";
        }

        try {
            JavaMailSender mailSender = buildMailSender(activeProfile);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(activeProfile.getEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            // ADD THESE LINES TEMPORARILY
            log.info("=== SMTP DEBUG ===");
            log.info("Host: {}", activeProfile.getSmtpHost());
            log.info("Port: {}", activeProfile.getSmtpPort());
            log.info("Username: {}", activeProfile.getEmail());
            log.info("Password length: {}", activeProfile.getAppPassword().length());
            log.info("==================");
            mailSender.send(message);
            log.info("Email sent to {} via {}", to, activeProfile.getEmail());
            return null; // null = success
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            return e.getMessage();
        }
    }

    private JavaMailSender buildMailSender(SmtpProfile profile) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(profile.getSmtpHost());
        mailSender.setPort(profile.getSmtpPort());
        mailSender.setUsername(profile.getEmail());
        mailSender.setPassword(profile.getAppPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }
}