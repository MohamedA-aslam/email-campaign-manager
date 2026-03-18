package com.campaign.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Campaign name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Subject line is required")
    @Column(nullable = false)
    private String subject;

    @NotBlank(message = "Email content is required")
    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DeliveryLog> deliveryLogs = new ArrayList<>();

    public enum CampaignStatus {
        DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED
    }

    @Entity
    @Table(name = "delivery_logs")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliveryLog {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "campaign_id", nullable = false)
        private Campaign campaign;

        @Column(name = "recipient_email", nullable = false)
        private String recipientEmail;

        @Column(name = "recipient_name")
        private String recipientName;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private DeliveryStatus status;

        @Column(name = "failure_reason")
        private String failureReason;

        @Column(name = "sent_at")
        @Builder.Default
        private LocalDateTime sentAt = LocalDateTime.now();

        public enum DeliveryStatus {
            SENT, FAILED
        }
    }
}