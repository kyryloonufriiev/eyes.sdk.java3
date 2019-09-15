package com.applitools.eyes;

import com.applitools.eyes.EyesScreenshot;

import java.util.List;

public interface IGetAccessibilityRegion {

    List<AccessibilityRegionByRectangle> getRegions(IEyesBase eyesBase, EyesScreenshot screenshot);
}
