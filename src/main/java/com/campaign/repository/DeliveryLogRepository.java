package com.campaign.repository;

import com.campaign.model.DeliveryLog;
import com.campaign.model.DeliveryLog.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {

    List<DeliveryLog> findByCampaignId(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, DeliveryStatus status);

    long countByCampaignId(Long campaignId);

    long countByStatus(DeliveryStatus status);
}