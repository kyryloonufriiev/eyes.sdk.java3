package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.*;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.TestResultContainer;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.ITestContext;
import org.testng.annotations.*;

public class TestSVGSites {
    private VisualGridRunner visualGridRunner;

    @BeforeClass
    public void beforeClass() {
        visualGridRunner = new VisualGridRunner(10);
        FileLogger logHandler = new FileLogger("SVGs.log", false, true);
        visualGridRunner.setLogHandler(logHandler);
        visualGridRunner.getLogger().log("enter");
    }

    @DataProvider(name = "dp", parallel = true)
    public static Object[][] dp() {
        return new Object[][]{
                {"https://carbon.sage.com/components/button-toggle-group"},
                {"https://wikipedia.org"},
                {"https://www.just-eat.co.uk/"},
        };
    }

    private Eyes initEyes(WebDriver webDriver, String testedUrl) {
        Eyes eyes = new Eyes(visualGridRunner);
        BatchInfo batchInfo = new BatchInfo("SVG Sites");
        batchInfo.setId("SVGs");
        eyes.setApiKey("97ELuwdIiAilbeumIilysV8yY24tygCeRFFTYEBO7EfE110");
        Logger logger = eyes.getLogger();
        logger.log("creating WebDriver: " + testedUrl);

        try {
            Configuration configuration = new Configuration();
            configuration.setAppName("Eyes Java SDK - SVG Sites");
            configuration.setTestName("testedUrl - "+testedUrl);
            configuration.setBatch(batchInfo);
            configuration.addBrowser(800, 600, BrowserType.CHROME);
            configuration.addBrowser(700, 500, BrowserType.FIREFOX);
            configuration.addBrowser(700, 500, BrowserType.IE_10);
            configuration.addBrowser(700, 500, BrowserType.IE_11);
            configuration.addBrowser(1600, 1200, BrowserType.CHROME);
            configuration.addBrowser(1200, 800, BrowserType.EDGE);
            logger.log("created configurations for url " + testedUrl);
            eyes.setConfiguration(configuration);
            eyes.open(webDriver);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return eyes;
    }

    @Test(dataProvider = "dp")
    public void test(String testedUrl) {
        visualGridRunner.getLogger().log("entering with url " + testedUrl);
        WebDriver webDriver = new ChromeDriver();
        webDriver.get(testedUrl);
        Eyes eyes = initEyes(webDriver, testedUrl);
        Logger logger = eyes.getLogger();
        logger.log("navigated to " + testedUrl);

        try {
            logger.log("running check for url " + testedUrl);
            try {
                eyes.check(Target.window().withName("Step1 - " + testedUrl).sendDom(true));
                eyes.check(Target.window().fully(false).withName("Step2 - " + testedUrl).sendDom(true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            eyes.getLogger().log("calling VisualGridEyes.close() for url " + testedUrl);
            eyes.closeAsync();
            logger.log("end of `try` block for url " + testedUrl);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        } finally {
            logger.log("closing WebDriver for url " + testedUrl);
            webDriver.quit();
            logger.log("url " + testedUrl + " - done with browser.");
            // End the test.
        }
    }

    @AfterMethod
    public void afterMethod(ITestContext testContext) {
        visualGridRunner.getLogger().log("enter");
    }

    @AfterClass
    public void afterClass(ITestContext testContext) {
        TestResultsSummary allTestResults = visualGridRunner.getAllTestResults();
        for (TestResultContainer allTestResult : allTestResults) {
            System.out.println(allTestResult.toString());
        }
        visualGridRunner.getLogger().log(allTestResults.toString());
    }
}
