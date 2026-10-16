package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.dto.DashboardStatsDto;
import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.service.DashboardService;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ExpiryDetectionService expiryDetectionService;
    private final ProductService productService;

    public DashboardController(DashboardService dashboardService,
                               ExpiryDetectionService expiryDetectionService,
                               ProductService productService) {
        this.dashboardService = dashboardService;
        this.expiryDetectionService = expiryDetectionService;
        this.productService = productService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        DashboardStatsDto stats = dashboardService.getDashboardStats(today);

        // Fetch top recent near-expiry & expired alerts for quick action table
        List<BatchItemViewDto> criticalBatches = expiryDetectionService.getAllActiveBatchViews(today).stream()
                .filter(b -> b.getDaysRemaining() <= 3)
                .sorted((a, b) -> Long.compare(a.getDaysRemaining(), b.getDaysRemaining()))
                .limit(6)
                .collect(Collectors.toList());

        // Fetch low-stock items for quick glance
        List<ProductViewDto> lowStockList = productService.getLowStockProducts(today).stream()
                .limit(6)
                .collect(Collectors.toList());

        model.addAttribute("stats", stats);
        model.addAttribute("criticalBatches", criticalBatches);
        model.addAttribute("lowStockList", lowStockList);
        model.addAttribute("currentDate", today);
        model.addAttribute("activeNav", "dashboard");

        return "dashboard/index";
    }

    @GetMapping("/api/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DashboardStatsDto> getDashboardStatsJson() {
        return ResponseEntity.ok(dashboardService.getDashboardStats(LocalDate.now()));
    }
}
