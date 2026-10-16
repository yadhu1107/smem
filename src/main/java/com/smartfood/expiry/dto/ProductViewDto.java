package com.smartfood.expiry.dto;

import com.smartfood.expiry.entity.Product;

import java.time.LocalDate;

public class ProductViewDto {

    private final Product product;
    private final int totalValidStock;
    private final int totalExpiredStock;
    private final int batchCount;
    private final LocalDate earliestExpiry;
    private final boolean lowStock;

    public ProductViewDto(Product product, int totalValidStock, int totalExpiredStock, int batchCount, LocalDate earliestExpiry) {
        this.product = product;
        this.totalValidStock = totalValidStock;
        this.totalExpiredStock = totalExpiredStock;
        this.batchCount = batchCount;
        this.earliestExpiry = earliestExpiry;
        this.lowStock = totalValidStock <= product.getMinStockQuantity();
    }

    public Product getProduct() {
        return product;
    }

    public int getTotalValidStock() {
        return totalValidStock;
    }

    public int getTotalExpiredStock() {
        return totalExpiredStock;
    }

    public int getBatchCount() {
        return batchCount;
    }

    public LocalDate getEarliestExpiry() {
        return earliestExpiry;
    }

    public boolean isLowStock() {
        return lowStock;
    }

    public boolean isOutOfStock() {
        return totalValidStock <= 0;
    }
}
