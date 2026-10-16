package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.dto.DashboardStatsDto;
import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final ExpiryDetectionService expiryDetectionService;
    private final ProductService productService;

    public DashboardService(ProductRepository productRepository,
                            ProductBatchRepository batchRepository,
                            CategoryRepository categoryRepository,
                            ExpiryDetectionService expiryDetectionService,
                            ProductService productService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.categoryRepository = categoryRepository;
        this.expiryDetectionService = expiryDetectionService;
        this.productService = productService;
    }

    public DashboardStatsDto getDashboardStats(LocalDate referenceDate) {
        DashboardStatsDto stats = new DashboardStatsDto();

        // 1. Total Products
        stats.setTotalProducts(productRepository.countByActiveTrue());

        // 2. Active Batches Views
        List<BatchItemViewDto> allBatchViews = expiryDetectionService.getAllActiveBatchViews(referenceDate);

        long totalStock = 0;
        long safeCount = 0;
        long nearCount = 0;
        long expiredCount = 0;
        BigDecimal valueAtRisk = BigDecimal.ZERO;

        long next1to3Days = 0;
        long next4to7Days = 0;
        long next8to14Days = 0;
        long next15to30Days = 0;
        long beyond30Days = 0;

        for (BatchItemViewDto view : allBatchViews) {
            ProductBatch b = view.getBatch();
            long days = view.getDaysRemaining();

            if (view.getStatus() == ExpiryStatus.EXPIRED) {
                expiredCount++;
                valueAtRisk = valueAtRisk.add(view.getTotalCost());
            } else {
                totalStock += b.getQuantity();

                if (view.getStatus() == ExpiryStatus.NEAR_EXPIRY) {
                    nearCount++;
                    valueAtRisk = valueAtRisk.add(view.getTotalCost());
                } else {
                    safeCount++;
                }

                // Upcoming expiries distribution
                if (days >= 0 && days <= 3) {
                    next1to3Days += b.getQuantity();
                } else if (days <= 7) {
                    next4to7Days += b.getQuantity();
                } else if (days <= 14) {
                    next8to14Days += b.getQuantity();
                } else if (days <= 30) {
                    next15to30Days += b.getQuantity();
                } else {
                    beyond30Days += b.getQuantity();
                }
            }
        }

        stats.setTotalStockQuantity(totalStock);
        stats.setNearingExpiryCount(nearCount);
        stats.setExpiredCount(expiredCount);
        stats.setStockValueAtRisk(valueAtRisk);

        // 3. Low stock products count
        List<ProductViewDto> lowStockProducts = productService.getLowStockProducts(referenceDate);
        stats.setLowStockCount(lowStockProducts.size());

        // 4. Status Counts Chart Data
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("Safe", safeCount);
        statusCounts.put("Near Expiry", nearCount);
        statusCounts.put("Expired", expiredCount);
        stats.setStatusCounts(statusCounts);

        // 5. Category Counts Chart Data
        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        for (Category cat : categories) {
            List<ProductBatch> catBatches = batchRepository.findByCategoryIdWithStock(cat.getId());
            long catStock = catBatches.stream()
                    .filter(b -> !b.getExpiryDate().isBefore(referenceDate))
                    .mapToLong(ProductBatch::getQuantity)
                    .sum();
            categoryCounts.put(cat.getName(), catStock);
        }
        stats.setCategoryCounts(categoryCounts);

        // 6. Upcoming Expiries Chart Data
        Map<String, Long> upcoming = new LinkedHashMap<>();
        upcoming.put("1 - 3 Days", next1to3Days);
        upcoming.put("4 - 7 Days", next4to7Days);
        upcoming.put("8 - 14 Days", next8to14Days);
        upcoming.put("15 - 30 Days", next15to30Days);
        upcoming.put("30+ Days", beyond30Days);
        stats.setUpcomingExpiriesMap(upcoming);

        return stats;
    }
}
