package com.applitools.eyes.selenium;

import com.applitools.eyes.*;
import com.applitools.eyes.metadata.SessionResults;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.utils.TestUtils;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Test;

import java.io.IOException;

public class AccessibilityTest {
    @Test
    public void TestAccessibilityRegions() {
        EyesRunner runner = new VisualGridRunner(3);
        Eyes newEyes = new Eyes(runner);
        Configuration config = newEyes.getConfiguration();
        config.setServerUrl("https://eyesfabric4eyes.applitools.com");
        config.setApiKey("CAE7aS103TDz7XyegELya3tHpEIXTFi0gBBwvgq104PSHIU110");
        config.setAccessibilityValidation(AccessibilityLevel.AAA);
        config.setProxy(new ProxySettings("http://127.0.0.1", 8888, null, null));
        newEyes.setConfiguration(config);
        WebDriver driver = new ChromeDriver();
        driver.get("https://applitools.github.io/demo/TestPages/FramesTestPage/");
        newEyes.open(driver, "Java SDK", "Accessibility", new RectangleSize(800, 600));
        newEyes.check(Target.window().accessibility(By.className("ignore"), AccessibilityRegionType.LargeText));
        TestResults close = newEyes.close();
        SessionResults resultObject = null;
        try {
            resultObject = TestUtils.getSessionResults(newEyes.getApiKey(), close);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        setExpectedAccessibilityRegions(new AccessibilityRegionByRectangle[]{
//                new AccessibilityRegionByRectangle(122, 928, 456, 306, AccessibilityRegionType.LargeText),
//                new AccessibilityRegionByRectangle(8, 1270, 690, 206, AccessibilityRegionType.LargeText),
//                new AccessibilityRegionByRectangle(10, 284, 800, 500, AccessibilityRegionType.LargeText)}
//        );
//        addExpectedProperty("AccessibilityLevel", AccessibilityLevel.AAA);
        System.out.println(resultObject);

    }
}
