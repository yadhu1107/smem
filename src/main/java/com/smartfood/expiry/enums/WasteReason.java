package com.smartfood.expiry.enums;

public enum WasteReason {
    EXPIRED("Product Past Expiration Date"),
    SPOILED("Spoiled / Rotten / Mold"),
    DAMAGED("Physical Container / Packaging Damaged"),
    PACKAGING_DEFECT("Defective Seal / Leak"),
    TEMPERATURE_ABUSE("Cold Chain Breakdown"),
    OTHER("Other Reason");

    private final String label;

    WasteReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
