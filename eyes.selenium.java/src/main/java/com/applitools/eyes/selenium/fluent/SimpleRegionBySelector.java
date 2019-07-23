package com.applitools.eyes.selenium.fluent;

import com.applitools.eyes.*;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.selenium.SeleniumEyes;
import com.applitools.eyes.selenium.rendering.IGetSeleniumRegion;
import org.openqa.selenium.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleRegionBySelector implements GetRegion , IGetSeleniumRegion {
    private By selector;

    public SimpleRegionBySelector(By selector) {
        this.selector = selector;
    }

    @Override
    public List<Region> getRegions(EyesBase eyesBase, EyesScreenshot screenshot) {
        List<WebElement> elements = ((SeleniumEyes) eyesBase).getDriver().findElements(this.selector);
        List<Region> values = new ArrayList<>(elements.size());
        for (WebElement element : elements) {

            Point locationAsPoint = element.getLocation();
            Dimension size = element.getSize();

            Location adjustedLocation;
            if (screenshot != null) {
                // Element's coordinates are context relative, so we need to convert them first.
                adjustedLocation = screenshot.getLocationInScreenshot(
                        new Location(locationAsPoint.getX(), locationAsPoint.getY()),
                        CoordinatesType.CONTEXT_RELATIVE);
            } else {
                adjustedLocation = new Location(locationAsPoint.getX(), locationAsPoint.getY());
            }
            values.add(new Region(adjustedLocation, new RectangleSize(size.getWidth(), size.getHeight()),
                    CoordinatesType.SCREENSHOT_AS_IS));
        }
        return values;
    }

    @Override
    public List<WebElement> getElements(WebDriver webDriver) {
        return webDriver.findElements(selector);
    }
}
