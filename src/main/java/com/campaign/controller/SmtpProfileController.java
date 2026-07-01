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

    @PostMapping
    public String addProfile(@Valid @ModelAttribute("newProfile") SmtpProfile profile,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("profiles", smtpProfileService.findAll());
            return "profile";
        }
        smtpProfileService.save(profile);
        redirectAttributes.addFlashAttribute("success", "SMTP profile added successfully.");
        return "redirect:/profile";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("profile", smtpProfileService.findById(id));
        return "profile-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateProfile(@PathVariable Long id,
                                @Valid @ModelAttribute("profile") SmtpProfile profile,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("profile", profile);
            return "profile-edit";
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