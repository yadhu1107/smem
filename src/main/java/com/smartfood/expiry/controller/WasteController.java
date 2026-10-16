package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.entity.WasteRecord;
import com.smartfood.expiry.enums.WasteReason;
import com.smartfood.expiry.service.BatchService;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.UserService;
import com.smartfood.expiry.service.WasteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/waste")
public class WasteController {

    private final WasteService wasteService;
    private final BatchService batchService;
    private final ExpiryDetectionService expiryDetectionService;
    private final UserService userService;

    public WasteController(WasteService wasteService,
                           BatchService batchService,
                           ExpiryDetectionService expiryDetectionService,
                           UserService userService) {
        this.wasteService = wasteService;
        this.batchService = batchService;
        this.expiryDetectionService = expiryDetectionService;
        this.userService = userService;
    }

    @GetMapping
    public String listWaste(Model model) {
        List<WasteRecord> wasteRecords = wasteService.getAllWasteRecords();
        model.addAttribute("wasteRecords", wasteRecords);
        model.addAttribute("totalLoss", wasteService.getTotalEstimatedLoss());
        model.addAttribute("totalQuantity", wasteService.getTotalWastedQuantity());
        model.addAttribute("activeNav", "waste");
        return "waste/list";
    }

    @GetMapping("/new")
    public String newWasteForm(@RequestParam(value = "batchId", required = false) Long batchId, Model model) {
        ProductBatch selectedBatch = null;
        if (batchId != null) {
            selectedBatch = batchService.getBatchById(batchId).orElse(null);
        }

        List<BatchItemViewDto> allActiveBatches = expiryDetectionService.getAllActiveBatchViews(LocalDate.now());

        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("batches", allActiveBatches);
        model.addAttribute("wasteReasons", WasteReason.values());
        model.addAttribute("defaultDate", LocalDate.now());
        model.addAttribute("activeNav", "waste");

        return "waste/form";
    }

    @PostMapping("/save")
    public String saveWaste(@RequestParam("batchId") Long batchId,
                            @RequestParam("quantity") Integer quantity,
                            @RequestParam("wasteReason") WasteReason wasteReason,
                            @RequestParam("wasteDate") String wasteDateStr,
                            @RequestParam(value = "notes", required = false) String notes,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            LocalDate wasteDate = (wasteDateStr != null && !wasteDateStr.isEmpty())
                    ? LocalDate.parse(wasteDateStr) : LocalDate.now();

            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            WasteRecord record = wasteService.recordWaste(batchId, quantity, wasteReason, wasteDate, notes, user);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Disposed " + quantity + " units of " + record.getBatch().getProduct().getName() +
                            " (Batch: " + record.getBatch().getBatchNumber() + "). Estimated cost loss: $" + record.getEstimatedCostLoss() + ".");

            return "redirect:/waste";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error recording waste: " + ex.getMessage());
            return "redirect:/waste/new" + (batchId != null ? "?batchId=" + batchId : "");
        }
    }
}
