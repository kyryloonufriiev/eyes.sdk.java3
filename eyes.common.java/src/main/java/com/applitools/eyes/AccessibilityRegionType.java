package com.applitools.eyes;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AccessibilityRegionType {
    None("0") ,
    RegularText("1") ,
    LargeText("2") ,
    BoldText("3") ,
    EssentialImage("4") ,
    DisabledOrInactive("5") ,
    NonEssentialImage("6"),
    Logo("7"),
    Background("8"),
    Ignore("9"),
    ;

    private String value;

    AccessibilityRegionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getMeters() {
        return value;
    }
}
