package com.campaign.repository;

import com.campaign.model.SmtpProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmtpProfileRepository extends JpaRepository<SmtpProfile, Long> {
    Optional<SmtpProfile> findByActiveTrue();
}