package com.smartfood.expiry.entity;

import com.smartfood.expiry.enums.WasteReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waste_records")
public class WasteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Batch selection is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductBatch batch;

    @NotNull(message = "Quantity wasted is required")
    @Min(value = 1, message = "Wasted quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Waste reason is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "waste_reason", nullable = false, length = 30)
    private WasteReason wasteReason;

    @NotNull(message = "Waste date is required")
    @Column(name = "waste_date", nullable = false)
    private LocalDate wasteDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedByUser;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "estimated_cost_loss", precision = 10, scale = 2)
    private BigDecimal estimatedCostLoss = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WasteRecord() {
    }

    public WasteRecord(ProductBatch batch, Integer quantity, WasteReason wasteReason,
                       LocalDate wasteDate, User recordedByUser, BigDecimal estimatedCostLoss, String notes) {
        this.batch = batch;
        this.quantity = quantity;
        this.wasteReason = wasteReason;
        this.wasteDate = wasteDate;
        this.recordedByUser = recordedByUser;
        this.estimatedCostLoss = estimatedCostLoss;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.wasteDate == null) {
            this.wasteDate = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductBatch batch) {
        this.batch = batch;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public WasteReason getWasteReason() {
        return wasteReason;
    }

    public void setWasteReason(WasteReason wasteReason) {
        this.wasteReason = wasteReason;
    }

    public LocalDate getWasteDate() {
        return wasteDate;
    }

    public void setWasteDate(LocalDate wasteDate) {
        this.wasteDate = wasteDate;
    }

    public User getRecordedByUser() {
        return recordedByUser;
    }

    public void setRecordedByUser(User recordedByUser) {
        this.recordedByUser = recordedByUser;
    }

    public BigDecimal getEstimatedCostLoss() {
        return estimatedCostLoss;
    }

    public void setEstimatedCostLoss(BigDecimal estimatedCostLoss) {
        this.estimatedCostLoss = estimatedCostLoss;
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
}
