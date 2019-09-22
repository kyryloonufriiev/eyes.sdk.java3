package com.applitools.eyes;

import com.applitools.AccessibilityLevel;

public class SessionAccessibilityStatus {
        private  AccessibilityStatus status;

    private AccessibilityLevel definitions;

    public AccessibilityStatus getStatus() {
        return status;
    }

    public void setStatus(AccessibilityStatus status) {
        this.status = status;
    }

    public AccessibilityLevel getDefinitions() {
        return definitions;
    }

    public void setDefinitions(AccessibilityLevel definitions) {
        this.definitions = definitions;
    }

    public enum AccessibilityStatus
    {
        None,

        Valid,

        Violation,
    }
}
