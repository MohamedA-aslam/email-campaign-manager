package com.campaign.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "smtp_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmtpProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Display name is required")
    @Column(name = "display_name", nullable = false)
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false)
    private String email;

    // SMTP fields
    @Column(name = "app_password")
    private String appPassword;

    @Column(name = "smtp_host")
    @Builder.Default
    private String smtpHost = "smtp.gmail.com";

    @Column(name = "smtp_port")
    @Builder.Default
    private Integer smtpPort = 587;

    // AWS SES fields ── NEW
    @Column(name = "aws_access_key")
    private String awsAccessKey;

    @Column(name = "aws_secret_key")
    private String awsSecretKey;

    @Column(name = "aws_region")
    @Builder.Default
    private String awsRegion = "us-east-1";

    // Provider
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Builder.Default
    private EmailProvider provider = EmailProvider.SMTP;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    public enum EmailProvider {
        SMTP,
        AMAZON_SES
    }
}