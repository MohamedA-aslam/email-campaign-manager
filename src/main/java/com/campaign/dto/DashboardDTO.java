package com.campaign.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardDTO {
    private long totalCampaigns;
    private long totalRecipients;
    private long totalEmailsSent;
    private long totalEmailsFailed;
    private List<CampaignDTO> campaigns;
}