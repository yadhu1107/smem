package com.smartfood.expiry.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BarcodeLookupDto {

    private boolean found;
    private String barcode;
    private Long productId;
    private String productName;
    private String categoryName;
    private Long categoryId;
    private String brand;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Integer minStockQuantity;
    private List<BatchSummaryDto> existingBatches = new ArrayList<>();

    public BarcodeLookupDto() {
    }

    public static class BatchSummaryDto {
        private Long id;
        private String batchNumber;
        private Integer quantity;
        private String expiryDate;
        private String status;

        public BatchSummaryDto(Long id, String batchNumber, Integer quantity, String expiryDate, String status) {
            this.id = id;
            this.batchNumber = batchNumber;
            this.quantity = quantity;
            this.expiryDate = expiryDate;
            this.status = status;
        }

        public Long getId() { return id; }
        public String getBatchNumber() { return batchNumber; }
        public Integer getQuantity() { return quantity; }
        public String getExpiryDate() { return expiryDate; }
        public String getStatus() { return status; }
    }

    // Getters and Setters
    public boolean isFound() { return found; }
    public void setFound(boolean found) { this.found = found; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Integer getMinStockQuantity() { return minStockQuantity; }
    public void setMinStockQuantity(Integer minStockQuantity) { this.minStockQuantity = minStockQuantity; }

    public List<BatchSummaryDto> getExistingBatches() { return existingBatches; }
    public void setExistingBatches(List<BatchSummaryDto> existingBatches) { this.existingBatches = existingBatches; }
}
