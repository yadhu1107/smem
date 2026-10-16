package com.smartfood.expiry.enums;

public enum MovementType {
    PURCHASE_IN("Stock Purchase / Inward"),
    SALE_OUT("Sale / Outward"),
    WASTE_OUT("Disposed as Waste"),
    DONATION_OUT("Donated to Charity"),
    ADJUSTMENT_IN("Inventory Audit Adjustment (+)"),
    ADJUSTMENT_OUT("Inventory Audit Adjustment (-)");

    private final String description;

    MovementType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
