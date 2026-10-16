package com.smartfood.expiry.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DashboardStatsDto {

    private long totalProducts;
    private long totalStockQuantity;
    private long nearingExpiryCount;
    private long expiredCount;
    private long lowStockCount;
    private BigDecimal stockValueAtRisk = BigDecimal.ZERO;

    private Map<String, Long> statusCounts = new HashMap<>();
    private Map<String, Long> categoryCounts = new HashMap<>();
    private Map<String, Long> upcomingExpiriesMap = new HashMap<>();

    public DashboardStatsDto() {
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getTotalStockQuantity() {
        return totalStockQuantity;
    }

    public void setTotalStockQuantity(long totalStockQuantity) {
        this.totalStockQuantity = totalStockQuantity;
    }

    public long getNearingExpiryCount() {
        return nearingExpiryCount;
    }

    public void setNearingExpiryCount(long nearingExpiryCount) {
        this.nearingExpiryCount = nearingExpiryCount;
    }

    public long getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(long expiredCount) {
        this.expiredCount = expiredCount;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public BigDecimal getStockValueAtRisk() {
        return stockValueAtRisk;
    }

    public void setStockValueAtRisk(BigDecimal stockValueAtRisk) {
        this.stockValueAtRisk = stockValueAtRisk;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public Map<String, Long> getUpcomingExpiriesMap() {
        return upcomingExpiriesMap;
    }

    public void setUpcomingExpiriesMap(Map<String, Long> upcomingExpiriesMap) {
        this.upcomingExpiriesMap = upcomingExpiriesMap;
    }
}
