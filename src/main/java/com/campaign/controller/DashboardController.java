package com.campaign.controller;

import com.campaign.dto.DashboardDTO;
import com.campaign.model.DeliveryLog.DeliveryStatus;
import com.campaign.repository.DeliveryLogRepository;
import com.campaign.repository.RecipientRepository;
import com.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CampaignService campaignService;
    private final RecipientRepository recipientRepository;
    private final DeliveryLogRepository deliveryLogRepository;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardDTO dto = DashboardDTO.builder()
                .totalCampaigns(campaignService.findAll().size())
                .totalRecipients(recipientRepository.count())
                .totalEmailsSent(deliveryLogRepository.countByStatus(DeliveryStatus.SENT))
                .totalEmailsFailed(deliveryLogRepository.countByStatus(DeliveryStatus.FAILED))
                .campaigns(campaignService.findAll().stream()
                        .map(campaignService::toCampaignDTO)
                        .toList())
                .build();
        model.addAttribute("dashboard", dto);
        return "dashboard";
    }
}