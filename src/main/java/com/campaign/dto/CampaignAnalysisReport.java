package com.campaign.dto;

public record CampaignAnalysisReport(
        long total,
        long sent,
        long failed,
        double deliveryRate,
        double failureRate,
        String segmentInsights,
        String failureBreakdown,
        String suggestions
) {}
