package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.DonationRecord;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.service.BatchService;
import com.smartfood.expiry.service.DonationService;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/donations")
public class DonationController {

    private final DonationService donationService;
    private final BatchService batchService;
    private final ExpiryDetectionService expiryDetectionService;
    private final UserService userService;

    public DonationController(DonationService donationService,
                              BatchService batchService,
                              ExpiryDetectionService expiryDetectionService,
                              UserService userService) {
        this.donationService = donationService;
        this.batchService = batchService;
        this.expiryDetectionService = expiryDetectionService;
        this.userService = userService;
    }

    @GetMapping
    public String listDonations(Model model) {
        List<DonationRecord> donations = donationService.getAllDonations();
        model.addAttribute("donations", donations);
        model.addAttribute("totalDonatedQuantity", donationService.getTotalDonatedQuantity());
        model.addAttribute("activeNav", "donations");
        return "donations/list";
    }

    @GetMapping("/new")
    public String newDonationForm(@RequestParam(value = "batchId", required = false) Long batchId, Model model) {
        ProductBatch selectedBatch = null;
        if (batchId != null) {
            selectedBatch = batchService.getBatchById(batchId).orElse(null);
        }

        // Strictly filter to eligible batches only (SAFE or NEAR_EXPIRY; never EXPIRED)
        List<BatchItemViewDto> eligibleBatches = expiryDetectionService.getAllActiveBatchViews(LocalDate.now()).stream()
                .filter(v -> v.getStatus() != ExpiryStatus.EXPIRED)
                .collect(Collectors.toList());

        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("batches", eligibleBatches);
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("activeNav", "donations");

        return "donations/form";
    }

    @PostMapping("/save")
    public String saveDonation(@RequestParam("batchId") Long batchId,
                               @RequestParam("recipientOrganization") String recipientOrganization,
                               @RequestParam("quantity") Integer quantity,
                               @RequestParam("donationDate") String donationDateStr,
                               @RequestParam(value = "notes", required = false) String notes,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            LocalDate donationDate = (donationDateStr != null && !donationDateStr.isEmpty())
                    ? LocalDate.parse(donationDateStr) : LocalDate.now();

            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            DonationRecord donation = donationService.recordDonation(batchId, recipientOrganization.trim(), quantity, donationDate, notes, user);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Recorded donation of " + quantity + " units to '" + donation.getRecipientOrganization() + "' successfully!");

            return "redirect:/donations";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Donation failed: " + ex.getMessage());
            return "redirect:/donations/new" + (batchId != null ? "?batchId=" + batchId : "");
        }
    }
}
