package com.applitools.eyes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;
import java.util.List;

public class AccessibilityRegionByRectangle implements IGetAccessibilityRegion {

    @JsonInclude
    private int left;
    @JsonInclude
    private int top;
    @JsonInclude
    private int width;
    @JsonInclude
    private int height;
    @JsonInclude
    private AccessibilityRegionType type;

    @JsonIgnore
    private Region region;

    public AccessibilityRegionByRectangle() {
    }

    public AccessibilityRegionByRectangle(int left, int top, int width, int height, AccessibilityRegionType regionType) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.type = regionType;
    }

    public AccessibilityRegionByRectangle(Region region, AccessibilityRegionType regionType) {
        this.type = regionType;
        this.setRegion(region);
    }

    public Region getRegion() {
        return new Region(left, top, width, height);
    }

    public void setRegion(Region region) {
        this.region = region;
        this.left =   region.getLeft();
        this.top =    region.getTop();
        this.width =  region.getWidth();
        this.height = region.getHeight();
    }


    public int getLeft() {
        return left;
    }

    public int getTop() {
        return top;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public AccessibilityRegionType getType() {
        return type;
    }

    public List<AccessibilityRegionByRectangle> GetRegions(IEyesBase eyesBase, EyesScreenshot screenshot) {
        return Arrays.asList(this);
    }

    @Override
    public List<AccessibilityRegionByRectangle> getRegions(IEyesBase eyesBase, EyesScreenshot screenshot) {
        return Arrays.asList(this);
    }
}

