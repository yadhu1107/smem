package com.smartfood.expiry.entity;

import com.smartfood.expiry.enums.ExpiryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "product_batches")
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank(message = "Batch number is required")
    @Size(max = 60, message = "Batch number cannot exceed 60 characters")
    @Column(name = "batch_number", nullable = false, length = 60)
    private String batchNumber;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Initial quantity is required")
    @Min(value = 0, message = "Initial quantity cannot be negative")
    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @NotNull(message = "Purchase date is required")
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @DecimalMin(value = "0.0", inclusive = true, message = "Cost per unit cannot be negative")
    @Column(name = "cost_per_unit", precision = 10, scale = 2)
    private BigDecimal costPerUnit = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price per unit cannot be negative")
    @Column(name = "selling_price_per_unit", precision = 10, scale = 2)
    private BigDecimal sellingPricePerUnit = BigDecimal.ZERO;

    @Size(max = 100)
    @Column(length = 100)
    private String supplier;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProductBatch() {
    }

    public ProductBatch(Product product, String batchNumber, Integer quantity, Integer initialQuantity,
                        LocalDate purchaseDate, LocalDate expiryDate, BigDecimal costPerUnit,
                        BigDecimal sellingPricePerUnit, String supplier) {
        this.product = product;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.initialQuantity = initialQuantity;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.costPerUnit = costPerUnit != null ? costPerUnit : BigDecimal.ZERO;
        this.sellingPricePerUnit = sellingPricePerUnit != null ? sellingPricePerUnit : BigDecimal.ZERO;
        this.supplier = supplier;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.initialQuantity == null && this.quantity != null) {
            this.initialQuantity = this.quantity;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Dynamic Expiry Calculations
    public long getDaysRemaining() {
        return getDaysRemaining(LocalDate.now());
    }

    public long getDaysRemaining(LocalDate referenceDate) {
        if (expiryDate == null) return 0;
        return ChronoUnit.DAYS.between(referenceDate, expiryDate);
    }

    public ExpiryStatus calculateStatus(int warningDays) {
        return calculateStatus(LocalDate.now(), warningDays);
    }

    public ExpiryStatus calculateStatus(LocalDate referenceDate, int warningDays) {
        if (expiryDate == null) return ExpiryStatus.SAFE;
        long days = ChronoUnit.DAYS.between(referenceDate, expiryDate);
        if (days < 0) {
            return ExpiryStatus.EXPIRED;
        } else if (days <= warningDays) {
            return ExpiryStatus.NEAR_EXPIRY;
        } else {
            return ExpiryStatus.SAFE;
        }
    }

    public BigDecimal getTotalCostValue() {
        if (quantity == null || costPerUnit == null) return BigDecimal.ZERO;
        return costPerUnit.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getTotalSellingValue() {
        if (quantity == null || sellingPricePerUnit == null) return BigDecimal.ZERO;
        return sellingPricePerUnit.multiply(BigDecimal.valueOf(quantity));
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(BigDecimal costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public BigDecimal getSellingPricePerUnit() {
        return sellingPricePerUnit;
    }

    public void setSellingPricePerUnit(BigDecimal sellingPricePerUnit) {
        this.sellingPricePerUnit = sellingPricePerUnit;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
