package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.WasteRecord;
import com.smartfood.expiry.service.DashboardService;
import com.smartfood.expiry.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final DashboardService dashboardService;

    public ReportController(ReportService reportService, DashboardService dashboardService) {
        this.reportService = reportService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String reportsPage(@RequestParam(value = "reportType", required = false, defaultValue = "CURRENT_INVENTORY") String reportType,
                              @RequestParam(value = "startDate", required = false) String startDateStr,
                              @RequestParam(value = "endDate", required = false) String endDateStr,
                              Model model) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDate.parse(startDateStr) : today.minusDays(30);
        LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDate.parse(endDateStr) : today;

        model.addAttribute("reportType", reportType.toUpperCase());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("activeNav", "reports");
        model.addAttribute("reportStats", dashboardService.getDashboardStats(today));

        switch (reportType.toUpperCase()) {
            case "CURRENT_INVENTORY":
                List<BatchItemViewDto> currentInv = reportService.getCurrentInventoryReport(today);
                model.addAttribute("reportData", currentInv);
                break;
            case "NEAR_EXPIRY":
                List<BatchItemViewDto> nearExpiry = reportService.getNearExpiryReport(today);
                model.addAttribute("reportData", nearExpiry);
                break;
            case "EXPIRED":
                List<BatchItemViewDto> expired = reportService.getExpiredStockReport(today);
                model.addAttribute("reportData", expired);
                break;
            case "LOW_STOCK":
                List<ProductViewDto> lowStock = reportService.getLowStockReport(today);
                model.addAttribute("reportData", lowStock);
                break;
            case "BATCH_HISTORY":
                List<StockMovement> movements = reportService.getBatchHistoryReport(startDate, endDate);
                model.addAttribute("reportData", movements);
                break;
            case "WASTE":
                List<WasteRecord> wasteRecords = reportService.getFoodWasteReport(startDate, endDate);
                model.addAttribute("reportData", wasteRecords);
                break;
            case "CATEGORY":
                List<ReportService.CategorySummaryDto> categorySummaries = reportService.getCategoryWiseReport(today);
                model.addAttribute("reportData", categorySummaries);
                break;
            default:
                break;
        }

        return "reports/index";
    }

    @GetMapping("/export/{reportType}")
    public ResponseEntity<byte[]> exportCsv(@PathVariable("reportType") String reportType,
                                            @RequestParam(value = "startDate", required = false) String startDateStr,
                                            @RequestParam(value = "endDate", required = false) String endDateStr) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) ? LocalDate.parse(startDateStr) : today.minusDays(30);
        LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty()) ? LocalDate.parse(endDateStr) : today;

        String csv = reportService.generateCsv(reportType, startDate, endDate);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = "report-" + reportType.toLowerCase() + "-" + today + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }
}
