package com.campaign.controller;

import com.campaign.dto.CampaignAnalysisReport;
import com.campaign.model.Campaign;
import com.campaign.service.AnthropicService;
import com.campaign.service.CampaignService;
import com.campaign.service.CampaignService.ExecutionResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final AnthropicService anthropicService;

    @GetMapping
    public String listCampaigns(Model model) {
        model.addAttribute("campaigns", campaignService.findAll().stream()
                .map(campaignService::toCampaignDTO)
                .toList());
        return "campaign-list";
    }

    @GetMapping("/new")
    public String newCampaignForm(Model model) {
        model.addAttribute("campaign", new Campaign());
        return "campaign-form";
    }

    @PostMapping
    public String createCampaign(@Valid @ModelAttribute Campaign campaign,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "campaign-form";
        campaignService.save(campaign);
        redirectAttributes.addFlashAttribute("success", "Campaign created successfully.");
        return "redirect:/campaigns";
    }

    @GetMapping("/{id}/edit")
    public String editCampaignForm(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignService.findById(id));
        return "campaign-form";
    }

    @PostMapping("/{id}")
    public String updateCampaign(@PathVariable Long id,
                                 @Valid @ModelAttribute Campaign campaign,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "campaign-form";
        campaignService.update(id, campaign);
        redirectAttributes.addFlashAttribute("success", "Campaign updated.");
        return "redirect:/campaigns";
    }

    @PostMapping("/{id}/delete")
    public String deleteCampaign(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        campaignService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Campaign deleted.");
        return "redirect:/campaigns";
    }

    @GetMapping("/{id}/detail")
    public String campaignDetail(@PathVariable Long id, Model model) {
        model.addAttribute("campaign", campaignService.toCampaignDTO(campaignService.findById(id)));
        model.addAttribute("logs", campaignService.getDeliveryLogs(id));
        return "campaign-detail";
    }

    @PostMapping("/{id}/send")
    public String sendNow(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Campaign campaign = campaignService.findById(id);
        ExecutionResult result = campaignService.executeCampaign(campaign);

        double deliveryRate = result.total() > 0
                ? Math.round(result.sent() * 1000.0 / result.total()) / 10.0 : 0.0;
        double failureRate = result.total() > 0
                ? Math.round(result.failed() * 1000.0 / result.total()) / 10.0 : 0.0;

        AnthropicService.InsightsResult insights = anthropicService.generateCampaignInsights(
                campaign.getName(), campaign.getSubject(),
                result.total(), result.sent(), result.failed(),
                result.failureReasons()
        );

        redirectAttributes.addFlashAttribute("analysis", new CampaignAnalysisReport(
                result.total(), result.sent(), result.failed(),
                deliveryRate, failureRate,
                insights.segmentInsights(),
                insights.failureBreakdown(),
                insights.suggestions()
        ));

        return "redirect:/campaigns/" + id + "/detail";
    }
}
