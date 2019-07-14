package com.applitools.eyes.selenium.fluent;

import com.applitools.eyes.*;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.selenium.rendering.IGetSeleniumRegion;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IgnoreRegionByElement implements GetRegion, IGetSeleniumRegion {
    private WebElement element;

    public IgnoreRegionByElement(WebElement element) {
        this.element = element;
    }

    @Override
    public List<Region> getRegions(EyesBase eyesBase, EyesScreenshot screenshot, boolean adjustLocation) {
        Point locationAsPoint = element.getLocation();
        Dimension size = element.getSize();

        Location adjustedLocation;
        if (screenshot != null) {
            // Element's coordinates are context relative, so we need to convert them first.
            adjustedLocation = screenshot.getLocationInScreenshot(new Location(locationAsPoint.getX(), locationAsPoint.getY()),
                    CoordinatesType.CONTEXT_RELATIVE);
        } else {
            adjustedLocation = new Location(locationAsPoint.getX(), locationAsPoint.getY());
        }

        List<Region> value = new ArrayList<>();
        value.add(new Region(adjustedLocation, new RectangleSize(size.getWidth(), size.getHeight()),
                CoordinatesType.SCREENSHOT_AS_IS));

        return value;
    }

    @Override
    public List<WebElement> getElements(WebDriver webDriver) {
        return Arrays.asList(element);
    }
}
