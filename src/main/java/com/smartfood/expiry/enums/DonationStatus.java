package com.smartfood.expiry.enums;

public enum DonationStatus {
    PENDING("Pending Pickup"),
    DISPATCHED("In Transit"),
    COMPLETED("Delivered & Accepted"),
    CANCELLED("Cancelled");

    private final String label;

    DonationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
