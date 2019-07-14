package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.FileLogger;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestRendersMatch {
    Eyes eyes = null;
    private BatchInfo testRendersMatch = new BatchInfo("TestRendersMatch");

    @Test
    public void TestSuccess() {
        VisualGridRunner visualGridRunner = new VisualGridRunner(10);
        visualGridRunner.setLogHandler(new FileLogger("TestRendersMatch.log", false, true));

        RectangleSize[] ViewportList = {
                new RectangleSize(800, 600),
                new RectangleSize(700, 500),
                new RectangleSize(1200, 800),
        };

        WebDriver webDriver = new ChromeDriver();
        webDriver.get("https://applitools.com/helloworld");

        try {
            for (RectangleSize viewport : ViewportList) {
                eyes = initEyes(null, webDriver, viewport, "TestSuccess");
                eyes.check(Target.window().fully());
                eyes.closeAsync();

                eyes = initEyes(visualGridRunner, webDriver, viewport, "TestSuccess");
                eyes.check(Target.window().fully());
                eyes.closeAsync();
            }
            TestResultsSummary results = visualGridRunner.getAllTestResults();
            Assert.assertEquals(ViewportList.length, results.getAllResults().length);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(visualGridRunner.getLogger(), e);
        } finally {
            webDriver.quit();
            eyes.abortIfNotClosed();
        }
    }

    @Test
    public void TestFailure() {
        final VisualGridRunner visualGridRunner = new VisualGridRunner(10);
        visualGridRunner.setLogHandler(new FileLogger("TestRendersMatch.log", false, true));

        final WebDriver webDriver = new ChromeDriver();

        RectangleSize[] ViewportList = {
                new RectangleSize(800, 600),
                new RectangleSize(700, 500),
                new RectangleSize(1200, 800),
        };
        webDriver.get("https://applitools.com/helloworld");
        for (RectangleSize viewport : ViewportList) {
            eyes = initEyes(null, webDriver, viewport, "TestFailure");
            eyes.check(Target.window().fully());
            eyes.closeAsync();

            eyes = initEyes(visualGridRunner, webDriver, viewport, "TestFailure");
            eyes.check(Target.window().fully());
            eyes.closeAsync();
            visualGridRunner.getAllTestResults();
        }
        webDriver.quit();
        eyes.abortIfNotClosed();
    }

    private Eyes initEyes(EyesRunner runner, WebDriver driver, RectangleSize viewport, String testName) {
        Eyes eyes = new Eyes(runner);

        Configuration sconf = new Configuration();
        sconf.setBatch(testRendersMatch);
        sconf.setViewportSize(viewport);
        sconf.setTestName(testName);
        sconf.setAppName("TestRendersMatch");
        eyes.setConfiguration(sconf);
        eyes.open(driver);
        return eyes;
    }
}

