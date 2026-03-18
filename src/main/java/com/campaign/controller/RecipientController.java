package com.campaign.controller;

import com.campaign.model.Campaign;
import com.campaign.model.Recipient;
import com.campaign.service.RecipientService;
import com.campaign.service.RecipientService.BulkUploadResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientService recipientService;

    @GetMapping
    public String listRecipients(Model model) {
        model.addAttribute("recipients", recipientService.findAll());
        model.addAttribute("newRecipient", new Recipient());
        return "recipients";
    }

    @PostMapping
    public String addRecipient(@Valid @ModelAttribute("newRecipient") Recipient recipient,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        recipientService.checkDuplicateEmail(null, recipient.getEmail(), result);
        if (result.hasErrors()) {
            model.addAttribute("recipients", recipientService.findAll());
            return "recipients";
        }
        try {
            recipientService.save(recipient);
            redirectAttributes.addFlashAttribute("success", "Recipient added.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recipients";
    }

    @PostMapping("/upload")
    public String uploadCsv(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file.");
            return "redirect:/recipients";
        }
        BulkUploadResult result = recipientService.uploadFromCsv(file);
        redirectAttributes.addFlashAttribute("success",
                "Uploaded: " + result.successCount() + " added, " + result.skippedCount() + " skipped.");
        if (!result.errors().isEmpty()) {
            redirectAttributes.addFlashAttribute("warnings", result.errors());
        }
        return "redirect:/recipients";
    }

    @GetMapping("/{id}/edit")
    public String editRecipient(@PathVariable Long id, Model model) {
        model.addAttribute("recipient", recipientService.findById(id));
        return "recipient-edit";
    }
    @PostMapping("/{id}")
    public String updateRecipient(@PathVariable Long id,
                                 @Valid @ModelAttribute Recipient recipient,
                                 BindingResult result, Model model,
                                 RedirectAttributes redirectAttributes) {
        recipientService.checkDuplicateEmail(id, recipient.getEmail(), result);
        if (result.hasErrors()) {
            model.addAttribute("recipient", recipient);
            return "recipient-edit";         // stay on the form, show inline errors
        }
        recipientService.update(id, recipient);
        redirectAttributes.addFlashAttribute("success", "Recipient updated.");
        return "redirect:/recipients";
    }


    @PostMapping("/{id}/delete")
    public String deleteRecipient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recipientService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Recipient deleted.");
        return "redirect:/recipients";
    }
}