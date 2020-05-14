package com.applitools.eyes;

public class SessionAccessibilityStatus {
    public enum AccessibilityStatus {
        PASSED,
        FAILED
    }

    private final AccessibilityStatus status;
    private final AccessibilitySettings settings;

    public SessionAccessibilityStatus(AccessibilityStatus status, AccessibilitySettings settings) {
        this.status = status;
        this.settings = settings;
    }

    public AccessibilityStatus getStatus() {
        return status;
    }

    public AccessibilitySettings getSettings() {
        return settings;
    }
}
