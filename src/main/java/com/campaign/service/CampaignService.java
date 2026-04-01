package com.campaign.service;

import com.campaign.dto.CampaignDTO;
import com.campaign.model.Campaign;
import com.campaign.model.Campaign.CampaignStatus;
import com.campaign.model.DeliveryLog;
import com.campaign.model.DeliveryLog.DeliveryStatus;
import com.campaign.model.Recipient;
import com.campaign.repository.CampaignRepository;
import com.campaign.repository.DeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final DeliveryLogRepository deliveryLogRepository;
    private final RecipientService recipientService;
    private final EmailService emailService;

    // Returned by executeCampaign() so callers can build an analysis report
    public record ExecutionResult(long total, long sent, long failed, List<String> failureReasons) {}

    public List<Campaign> findAll() {
        return campaignRepository.findAll();
    }

    public Campaign findById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Campaign not found: " + id));
    }

    public Campaign save(Campaign campaign) {
        if (campaign.getScheduledTime() != null) {
            campaign.setStatus(CampaignStatus.SCHEDULED);
        }
        return campaignRepository.save(campaign);
    }

    public Campaign update(Long id, Campaign updated) {
        Campaign existing = findById(id);
        existing.setName(updated.getName());
        existing.setSubject(updated.getSubject());
        existing.setContent(updated.getContent());
        existing.setScheduledTime(updated.getScheduledTime());
        if (updated.getScheduledTime() != null && existing.getStatus() == CampaignStatus.DRAFT) {
            existing.setStatus(CampaignStatus.SCHEDULED);
        }
        return campaignRepository.save(existing);
    }

    public void deleteById(Long id) {
        campaignRepository.deleteById(id);
    }

    @Transactional
    public ExecutionResult executeCampaign(Campaign campaign) {
        log.info("Executing campaign: {}", campaign.getName());
        campaign.setStatus(CampaignStatus.IN_PROGRESS);
        campaignRepository.save(campaign);

        List<Recipient> recipients = recipientService.findSubscribed();
        long sentCount = 0;
        long failedCount = 0;
        List<String> failureReasons = new ArrayList<>();

        for (Recipient recipient : recipients) {
            String failureReason = emailService.sendEmail(
                    recipient.getEmail(),
                    campaign.getSubject(),
                    campaign.getContent()
            );

            DeliveryLog deliveryLog = DeliveryLog.builder()
                    .campaign(campaign)
                    .recipientEmail(recipient.getEmail())
                    .recipientName(recipient.getName())
                    .status(failureReason == null ? DeliveryStatus.SENT : DeliveryStatus.FAILED)
                    .failureReason(failureReason)
                    .build();

            deliveryLogRepository.save(deliveryLog);

            if (failureReason == null) {
                sentCount++;
            } else {
                failedCount++;
                failureReasons.add(failureReason);
            }
        }

        campaign.setStatus(CampaignStatus.COMPLETED);
        campaignRepository.save(campaign);
        log.info("Campaign completed: {} — sent={}, failed={}", campaign.getName(), sentCount, failedCount);

        return new ExecutionResult(recipients.size(), sentCount, failedCount, failureReasons);
    }

    public CampaignDTO toCampaignDTO(Campaign campaign) {
        long total = deliveryLogRepository.countByCampaignId(campaign.getId());
        long sent = deliveryLogRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.SENT);
        long failed = deliveryLogRepository.countByCampaignIdAndStatus(campaign.getId(), DeliveryStatus.FAILED);

        return CampaignDTO.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .subject(campaign.getSubject())
                .content(campaign.getContent())
                .scheduledTime(campaign.getScheduledTime())
                .status(campaign.getStatus())
                .totalRecipients(total)
                .sentCount(sent)
                .failedCount(failed)
                .build();
    }

    public List<DeliveryLog> getDeliveryLogs(Long campaignId) {
        return deliveryLogRepository.findByCampaignId(campaignId);
    }
}
