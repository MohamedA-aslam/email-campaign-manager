package com.campaign.service;

import com.campaign.model.Campaign;
import com.campaign.model.Recipient;
import com.campaign.model.Recipient.SubscriptionStatus;
import com.campaign.repository.RecipientRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientService {

    private final RecipientRepository recipientRepository;

    public List<Recipient> findAll() {
        return recipientRepository.findAll();
    }

    public List<Recipient> findSubscribed() {
        return recipientRepository.findBySubscriptionStatus(SubscriptionStatus.SUBSCRIBED);
    }

    public Recipient findById(Long id) {
        return recipientRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recipient not found: " + id));
    }

    public Recipient save(Recipient recipient) {
        return recipientRepository.save(recipient);
    }

    public Recipient update(Long id, Recipient updated){
        Recipient existing = findById(id);
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setSubscriptionStatus(updated.getSubscriptionStatus());
        return recipientRepository.save(existing);
    }

    public void deleteById(Long id) {
        recipientRepository.deleteById(id);
    }

    /**
     * Bulk upload recipients from CSV.
     * Expected CSV columns: name, email, subscription_status
     */
    public BulkUploadResult uploadFromCsv(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String[] headers = reader.readNext(); // skip header row
            if (headers == null) {
                errors.add("CSV file is empty.");
                return new BulkUploadResult(0, 0, errors);
            }

            String[] line;
            int lineNumber = 1;

            while ((line = reader.readNext()) != null) {
                lineNumber++;
                if (line.length < 2) {
                    errors.add("Line " + lineNumber + ": Insufficient columns.");
                    continue;
                }

                String name = line[0].trim();
                String email = line[1].trim();
                String statusStr = line.length > 2 ? line[2].trim() : "SUBSCRIBED";

                // Validate email format
                if (!isValidEmail(email)) {
                    errors.add("Line " + lineNumber + ": Invalid email format — " + email);
                    continue;
                }

                // Check duplicate
                if (recipientRepository.existsByEmail(email)) {
                    skippedCount++;
                    log.info("Skipped duplicate email: {}", email);
                    continue;
                }

                SubscriptionStatus status;
                try {
                    status = SubscriptionStatus.valueOf(statusStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    status = SubscriptionStatus.SUBSCRIBED;
                }

                Recipient recipient = Recipient.builder()
                        .name(name)
                        .email(email)
                        .subscriptionStatus(status)
                        .build();

                recipientRepository.save(recipient);
                successCount++;
            }

        } catch (IOException | CsvValidationException e) {
            log.error("Error reading CSV file", e);
            errors.add("Failed to read CSV: " + e.getMessage());
        }

        return new BulkUploadResult(successCount, skippedCount, errors);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public void checkDuplicateEmail(Long currentId, String email, BindingResult result) {
        recipientRepository.findByEmail(email).ifPresent(existing -> {
            boolean isSameRecipient = existing.getId().equals(currentId);
            if (!isSameRecipient) {
                result.rejectValue("email","email.duplicate","This email address is already registered.");
            }
        });
    }

    public record BulkUploadResult(int successCount, int skippedCount, List<String> errors) {}
}