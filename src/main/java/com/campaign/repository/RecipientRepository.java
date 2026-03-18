package com.campaign.repository;

import com.campaign.model.Recipient;
import com.campaign.model.Recipient.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {

    Optional<Recipient> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Recipient> findBySubscriptionStatus(SubscriptionStatus status);
}