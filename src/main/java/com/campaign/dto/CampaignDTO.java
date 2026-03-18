package com.campaign.dto;

import com.campaign.model.Campaign.CampaignStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CampaignDTO {
    private Long id;
    private String name;
    private String subject;
    private String content;
    private LocalDateTime scheduledTime;
    private CampaignStatus status;
    private long totalRecipients;
    private long sentCount;
    private long failedCount;
}
