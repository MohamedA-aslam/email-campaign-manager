package com.campaign.controller;

import com.campaign.model.SmtpProfile;
import com.campaign.service.SmtpProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class SmtpProfileController {

    private final SmtpProfileService smtpProfileService;

    @GetMapping
    public String profilePage(Model model) {
        model.addAttribute("profiles", smtpProfileService.findAll());
        model.addAttribute("newProfile", new SmtpProfile());
        return "profile";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("profile", smtpProfileService.findById(id));
        return "profile-edit";
    }
    @PostMapping
    public String addProfile(@ModelAttribute("newProfile") SmtpProfile profile,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        // Manual validation based on provider
        if (profile.getProvider() == SmtpProfile.EmailProvider.AMAZON_SES) {
            if (profile.getAwsAccessKey() == null || profile.getAwsAccessKey().isBlank()) {
                result.rejectValue("awsAccessKey", "required", "AWS Access Key is required");
            }
            if (profile.getAwsSecretKey() == null || profile.getAwsSecretKey().isBlank()) {
                result.rejectValue("awsSecretKey", "required", "AWS Secret Key is required");
            }
            if (profile.getAwsRegion() == null || profile.getAwsRegion().isBlank()) {
                result.rejectValue("awsRegion", "required", "AWS Region is required");
            }
            // Clear SMTP fields for SES profiles
            profile.setAppPassword("N/A");
            profile.setSmtpHost("AWS SES");
        } else {
            if (profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
                result.rejectValue("displayName", "required", "Display name is required");
            }
            if (profile.getEmail() == null || profile.getEmail().isBlank()) {
                result.rejectValue("email", "required", "Email is required");
            }
            if (profile.getAppPassword() == null || profile.getAppPassword().isBlank()) {
                result.rejectValue("appPassword", "required", "App password is required");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("profiles", smtpProfileService.findAll());
            return "profile";
        }

        smtpProfileService.save(profile);
        redirectAttributes.addFlashAttribute("success", "Email profile added successfully.");
        return "redirect:/profile";
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(@PathVariable Long id,
                                @ModelAttribute("profile") SmtpProfile profile,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {

        if (profile.getProvider() == SmtpProfile.EmailProvider.AMAZON_SES) {
            if (profile.getAwsAccessKey() == null || profile.getAwsAccessKey().isBlank()) {
                result.rejectValue("awsAccessKey", "required", "AWS Access Key is required");
            }
            if (profile.getAwsSecretKey() == null || profile.getAwsSecretKey().isBlank()) {
                result.rejectValue("awsSecretKey", "required", "AWS Secret Key is required");
            }
            if (result.hasErrors()) {
                model.addAttribute("profile", profile);
                return "profile-edit";
            }
            profile.setAppPassword("N/A");
            profile.setSmtpHost("AWS SES");
        }

        smtpProfileService.update(id, profile);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        return "redirect:/profile";
    }
    @PostMapping("/{id}/activate")
    public String activateProfile(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        smtpProfileService.setActive(id);
        redirectAttributes.addFlashAttribute("success", "SMTP profile activated.");
        return "redirect:/profile";
    }

    @PostMapping("/{id}/delete")
    public String deleteProfile(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        smtpProfileService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Profile deleted.");
        return "redirect:/profile";
    }
}