package com.applitools.eyes;

public class SubregionForStitching {
    private final Location scrollTo;
    private final Location pastePhysicalLocation;
    private final Region physicalCropArea;
    private final Region logicalCropArea;

    public SubregionForStitching(Location scrollTo, Location pastePhysicalLocation, Region physicalCropArea, Region logicalCropArea) {

        this.scrollTo = scrollTo;
        this.pastePhysicalLocation = pastePhysicalLocation;
        this.physicalCropArea = physicalCropArea;
        this.logicalCropArea = logicalCropArea;
    }

    public Location getScrollTo() {
        return scrollTo;
    }

    public Location getPastePhysicalLocation() {
        return pastePhysicalLocation;
    }

    public Region getPhysicalCropArea() {
        return physicalCropArea;
    }

    public Region getLogicalCropArea() {
        return logicalCropArea;
    }
}
