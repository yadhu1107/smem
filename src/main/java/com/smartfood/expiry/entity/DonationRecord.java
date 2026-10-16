package com.smartfood.expiry.entity;

import com.smartfood.expiry.enums.DonationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donation_records")
public class DonationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Batch selection is required")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductBatch batch;

    @NotBlank(message = "Recipient organization is required")
    @Size(max = 150)
    @Column(name = "recipient_organization", nullable = false, length = 150)
    private String recipientOrganization;

    @NotNull(message = "Donation quantity is required")
    @Min(value = 1, message = "Donated quantity must be at least 1")
    @Column(nullable = false)
    private Integer quantity;

    @NotNull(message = "Donation date is required")
    @Column(name = "donation_date", nullable = false)
    private LocalDate donationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status = DonationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedByUser;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DonationRecord() {
    }

    public DonationRecord(ProductBatch batch, String recipientOrganization, Integer quantity,
                          LocalDate donationDate, DonationStatus status, User recordedByUser, String notes) {
        this.batch = batch;
        this.recipientOrganization = recipientOrganization;
        this.quantity = quantity;
        this.donationDate = donationDate;
        this.status = status != null ? status : DonationStatus.PENDING;
        this.recordedByUser = recordedByUser;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.donationDate == null) {
            this.donationDate = LocalDate.now();
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

    public String getRecipientOrganization() {
        return recipientOrganization;
    }

    public void setRecipientOrganization(String recipientOrganization) {
        this.recipientOrganization = recipientOrganization;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public User getRecordedByUser() {
        return recordedByUser;
    }

    public void setRecordedByUser(User recordedByUser) {
        this.recordedByUser = recordedByUser;
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
