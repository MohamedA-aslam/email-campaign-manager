package com.campaign.service;

import com.campaign.model.Campaign;
import com.campaign.model.Campaign.CampaignStatus;
import com.campaign.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignSchedulerService {

    private final CampaignRepository campaignRepository;
    private final CampaignService campaignService;

    /**
     * Checks every minute for campaigns that are due to be sent.
     */
    @Scheduled(fixedDelay = 10000)
    public void processDueCampaigns() {
        List<Campaign> dueCampaigns = campaignRepository
                .findByStatusAndScheduledTimeBefore(CampaignStatus.SCHEDULED, LocalDateTime.now());

        if (!dueCampaigns.isEmpty()) {
            log.info("Found {} campaign(s) due for execution.", dueCampaigns.size());
            for (Campaign campaign : dueCampaigns) {
                try {
                    campaignService.executeCampaign(campaign);
                } catch (Exception e) {
                    log.error("Error executing campaign {}: {}", campaign.getId(), e.getMessage(), e);
                }
            }
        }
    }
}