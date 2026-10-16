package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.ExpiryNotification;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.FefoService;
import com.smartfood.expiry.service.NotificationService;
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
@RequestMapping("/expiry")
public class ExpiryAlertController {

    private final ExpiryDetectionService expiryDetectionService;
    private final FefoService fefoService;
    private final NotificationService notificationService;
    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public ExpiryAlertController(ExpiryDetectionService expiryDetectionService,
                                 FefoService fefoService,
                                 NotificationService notificationService,
                                 CategoryRepository categoryRepository,
                                 UserService userService) {
        this.expiryDetectionService = expiryDetectionService;
        this.fefoService = fefoService;
        this.notificationService = notificationService;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @GetMapping("/alerts")
    public String expiryAlerts(@RequestParam(value = "filter", required = false, defaultValue = "ALL") String filter,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               Model model) {
        LocalDate today = LocalDate.now();
        List<BatchItemViewDto> allViews = expiryDetectionService.getAllActiveBatchViews(today);

        // Filter by category if specified
        if (categoryId != null) {
            allViews = allViews.stream()
                    .filter(v -> v.getBatch().getProduct().getCategory() != null &&
                            v.getBatch().getProduct().getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        List<BatchItemViewDto> expired = allViews.stream()
                .filter(v -> v.getStatus() == ExpiryStatus.EXPIRED)
                .collect(Collectors.toList());

        List<BatchItemViewDto> expiringToday = allViews.stream()
                .filter(v -> v.getDaysRemaining() == 0)
                .collect(Collectors.toList());

        List<BatchItemViewDto> nearExpiry = allViews.stream()
                .filter(v -> v.getStatus() == ExpiryStatus.NEAR_EXPIRY)
                .collect(Collectors.toList());

        List<ExpiryNotification> unreviewedNotifications = notificationService.getUnreviewedNotifications();

        model.addAttribute("allCount", expired.size() + nearExpiry.size());
        model.addAttribute("expiredBatches", expired);
        model.addAttribute("expiringTodayBatches", expiringToday);
        model.addAttribute("nearExpiryBatches", nearExpiry);
        model.addAttribute("unreviewedNotifications", unreviewedNotifications);
        model.addAttribute("selectedFilter", filter);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("today", today);
        model.addAttribute("activeNav", "expiry-alerts");

        return "expiry/alerts";
    }

    @GetMapping("/fefo")
    public String fefoRecommendations(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                      Model model) {
        LocalDate today = LocalDate.now();
        List<BatchItemViewDto> fefoList = fefoService.getFefoRecommendations(today);

        if (categoryId != null) {
            fefoList = fefoList.stream()
                    .filter(v -> v.getBatch().getProduct().getCategory() != null &&
                            v.getBatch().getProduct().getCategory().getId().equals(categoryId))
                    .collect(Collectors.toList());
        }

        model.addAttribute("fefoList", fefoList);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("today", today);
        model.addAttribute("activeNav", "fefo");

        return "expiry/fefo";
    }

    @PostMapping("/review/{notificationId}")
    public String markNotificationReviewed(@PathVariable("notificationId") Long notificationId,
                                           @RequestParam(value = "notes", required = false) String notes,
                                           @AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        try {
            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;
            notificationService.markAsReviewed(notificationId, user, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Notification marked as reviewed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to review alert: " + ex.getMessage());
        }
        return "redirect:/expiry/alerts";
    }

    @PostMapping("/check-now")
    public String triggerManualExpiryCheck(RedirectAttributes redirectAttributes) {
        int newAlerts = notificationService.performExpiryCheck(LocalDate.now());
        redirectAttributes.addFlashAttribute("successMessage",
                "Expiry audit completed successfully! Identified " + newAlerts + " new alert items requiring attention.");
        return "redirect:/expiry/alerts";
    }
}
