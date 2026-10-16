package com.smartfood.expiry.enums;

public enum ExpiryStatus {
    SAFE("Safe", "success", "bi-check-circle-fill"),
    NEAR_EXPIRY("Near Expiry", "warning", "bi-exclamation-triangle-fill"),
    EXPIRED("Expired", "danger", "bi-x-circle-fill");

    private final String displayName;
    private final String badgeClass;
    private final String iconClass;

    ExpiryStatus(String displayName, String badgeClass, String iconClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.iconClass = iconClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIconClass() {
        return iconClass;
    }
}
