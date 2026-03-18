package com.campaign.repository;

import com.campaign.model.Campaign;
import com.campaign.model.Campaign.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByStatus(CampaignStatus status);

    List<Campaign> findByStatusAndScheduledTimeBefore(CampaignStatus status, LocalDateTime time);
}