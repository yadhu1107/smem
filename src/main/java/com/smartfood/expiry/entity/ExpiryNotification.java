package com.smartfood.expiry.entity;

import com.smartfood.expiry.enums.ExpiryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expiry_notifications")
public class ExpiryNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductBatch batch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private ExpiryStatus alertType;

    @NotNull
    @Column(name = "alert_date", nullable = false)
    private LocalDate alertDate;

    @Column(name = "days_remaining", nullable = false)
    private Long daysRemaining;

    @Column(name = "is_reviewed", nullable = false)
    private boolean reviewed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedByUser;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String notes;

    public ExpiryNotification() {
    }

    public ExpiryNotification(ProductBatch batch, ExpiryStatus alertType, LocalDate alertDate, Long daysRemaining) {
        this.batch = batch;
        this.alertType = alertType;
        this.alertDate = alertDate;
        this.daysRemaining = daysRemaining;
        this.reviewed = false;
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

    public ExpiryStatus getAlertType() {
        return alertType;
    }

    public void setAlertType(ExpiryStatus alertType) {
        this.alertType = alertType;
    }

    public LocalDate getAlertDate() {
        return alertDate;
    }

    public void setAlertDate(LocalDate alertDate) {
        this.alertDate = alertDate;
    }

    public Long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(Long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public User getReviewedByUser() {
        return reviewedByUser;
    }

    public void setReviewedByUser(User reviewedByUser) {
        this.reviewedByUser = reviewedByUser;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
