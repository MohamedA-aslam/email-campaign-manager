package com.campaign.service;

import com.campaign.model.SmtpProfile;
import com.campaign.repository.SmtpProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpProfileService {

    private final SmtpProfileRepository smtpProfileRepository;

    public List<SmtpProfile> findAll() {
        return smtpProfileRepository.findAll();
    }

    public SmtpProfile findById(Long id) {
        return smtpProfileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SMTP Profile not found: " + id));
    }

    public SmtpProfile save(SmtpProfile profile) {
        return smtpProfileRepository.save(profile);
    }

    public void deleteById(Long id) {
        smtpProfileRepository.deleteById(id);
    }

    public SmtpProfile update(Long id, SmtpProfile updated) {
        SmtpProfile existing = findById(id);
        existing.setDisplayName(updated.getDisplayName());
        existing.setEmail(updated.getEmail());
        existing.setAppPassword(updated.getAppPassword());
        existing.setSmtpHost(updated.getSmtpHost());
        existing.setSmtpPort(updated.getSmtpPort());
        return smtpProfileRepository.save(existing);
    }

    /**
     * Returns the currently active SMTP profile.
     * Returns null if no profile is active — caller is responsible for handling this.
     */
    public SmtpProfile getActiveProfile() {
        return smtpProfileRepository.findByActiveTrue().orElse(null);
    }

    @Transactional
    public void setActive(Long id) {
        // Deactivate all profiles first
        List<SmtpProfile> all = smtpProfileRepository.findAll();
        all.forEach(p -> p.setActive(false));
        smtpProfileRepository.saveAll(all);

        // Activate selected profile
        SmtpProfile selected = findById(id);
        selected.setActive(true);
        smtpProfileRepository.save(selected);
        log.info("SMTP profile activated: {}", selected.getEmail());
    }
}