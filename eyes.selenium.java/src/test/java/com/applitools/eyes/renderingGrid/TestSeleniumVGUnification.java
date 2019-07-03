package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.Logger;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.EyesRunner;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public final class TestSeleniumVGUnification {

    private EyesRunner runner;
    private WebDriver webDriver;

    @BeforeMethod
    public void Before(ITestContext testContext){
        runner = new VisualGridRunner(1);

        webDriver = new ChromeDriver();
        webDriver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage");
        //webDriver.get("http://applitools-vg-test.surge.sh/test.html");
    }

    @Test
    public void test() {

        Eyes eyes = new Eyes(runner);
        eyes.setLogHandler(new StdoutLogHandler());
        eyes.setBatch(new BatchInfo("SimpleVisualGridBatch"));

        try {
//            eyes.setProxy(new ProxySettings("http://127.0.0.1", 8888, null, null));
            eyes.open(webDriver, "Test for Selenium unification", "test1");
            eyes.check(Target.window().withName("test").fully(false).sendDom(false));
            TestResults close = eyes.close();
            Assert.assertNotNull(close);

        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(eyes.getLogger(), e);
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
            // End the test.
        }
    }

    @AfterMethod
    public void After(ITestContext testContext) {
        Logger logger = runner.getLogger();
        TestResultsSummary allTestResults = runner.getAllTestResults();
        logger.log(allTestResults.toString());
        webDriver.quit();
    }
}