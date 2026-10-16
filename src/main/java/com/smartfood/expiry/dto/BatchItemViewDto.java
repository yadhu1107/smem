package com.smartfood.expiry.dto;

import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BatchItemViewDto {

    private final ProductBatch batch;
    private final long daysRemaining;
    private final ExpiryStatus status;
    private final int warningDaysUsed;

    public BatchItemViewDto(ProductBatch batch, LocalDate referenceDate, int warningDays) {
        this.batch = batch;
        this.daysRemaining = batch.getDaysRemaining(referenceDate);
        this.warningDaysUsed = warningDays;
        this.status = batch.calculateStatus(referenceDate, warningDays);
    }

    public ProductBatch getBatch() {
        return batch;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public ExpiryStatus getStatus() {
        return status;
    }

    public int getWarningDaysUsed() {
        return warningDaysUsed;
    }

    public String getStatusBadgeClass() {
        return status.getBadgeClass();
    }

    public String getStatusDisplayName() {
        return status.getDisplayName();
    }

    public String getStatusIconClass() {
        return status.getIconClass();
    }

    public String getDaysRemainingText() {
        if (daysRemaining < 0) {
            return Math.abs(daysRemaining) + " days ago";
        } else if (daysRemaining == 0) {
            return "Expires Today!";
        } else if (daysRemaining == 1) {
            return "1 day left";
        } else {
            return daysRemaining + " days left";
        }
    }

    public BigDecimal getTotalCost() {
        return batch.getTotalCostValue();
    }

    public BigDecimal getTotalSelling() {
        return batch.getTotalSellingValue();
    }
}
