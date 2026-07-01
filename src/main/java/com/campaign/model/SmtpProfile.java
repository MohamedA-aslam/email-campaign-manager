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

    @NotBlank(message = "App password is required")
    @Column(name = "app_password", nullable = false)
    private String appPassword;

    @NotBlank(message = "SMTP host is required")
    @Column(name = "smtp_host", nullable = false)
    @Builder.Default
    private String smtpHost = "smtp.gmail.com";

    @NotNull(message = "SMTP port is required")
    @Column(name = "smtp_port", nullable = false)
    @Builder.Default
    private Integer smtpPort = 587;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;
}