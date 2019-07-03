package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.*;
import com.applitools.eyes.selenium.BrowserType;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.model.*;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TestMissingContentSites {
    private VisualGridRunner visualGridRunner;

    private String logsPath = System.getenv("APPLITOOLS_LOGS_PATH");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
    private String dateTimeString = dateFormat.format(Calendar.getInstance().getTime());

    @BeforeClass
    public void beforeClass() {
        visualGridRunner = new VisualGridRunner(10);
//        visualGridRunner.setLogHandler(new StdoutLogHandler(true));
        FileLogger logHandler = new FileLogger("missingContent.log", false, true);
        visualGridRunner.setLogHandler(logHandler);
        visualGridRunner.getLogger().log("enter");
    }

    @DataProvider(name = "dp", parallel = true)
    public static Object[][] dp() {
        return new Object[][]{
//                {"https://www.verizonwireless.com/"},
                {"https://cvshealth.com/"},
//                {"https://www.walgreens.com/"},
//                {"https://www.tesla.com/"},
//                {"https://www.walgreens.com/"},
        };
    }

    private Eyes initEyes(WebDriver webDriver, String testedUrl) {
        Eyes eyes = new Eyes(visualGridRunner);
        initLogging(testedUrl, eyes);

        Logger logger = eyes.getLogger();
        logger.log("creating WebDriver: " + testedUrl);

        try {
            Configuration configuration = new Configuration();
            configuration.setTestName(testedUrl);
            configuration.setAppName("Java SDK");
            configuration.setBatch(new BatchInfo("Missing content Issue"));
            configuration.addBrowser(1200, 800, BrowserType.CHROME);
            configuration.addBrowser(1200, 800, BrowserType.EDGE);
//            configuration.addDeviceEmulation(emulation);
            logger.log("created configurations for url " + testedUrl);
            //VisualGridEyes.setServerUrl("https://eyes.applitools.com/");
//            configuration.setProxy(new ProxySettings("http://127.0.0.1", 8888, null, null));
            eyes.setConfiguration(configuration);
            eyes.open(webDriver, "Michael's App", "First Test", new RectangleSize(1200, 800));
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
        Eyes eyes = (Eyes) initEyes(webDriver, testedUrl);
        Logger logger = eyes.getLogger();
        logger.log("navigated to " + testedUrl);

        try {
            //CheckRGSettings setting = new CheckRGSettings(CheckRGSettings.SizeMode.FULL_PAGE, null, null, false);
            logger.log("running check for url " + testedUrl);
            try {
                eyes.check(Target.window().withName("Step1 - " + testedUrl).sendDom(true));
                eyes.check(Target.window().fully(false).withName("Step2 - " + testedUrl).sendDom(true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            eyes.getLogger().log("calling VisualGridEyes.close() for url " + testedUrl);
//            VisualGridEyes.close();
//            TestResults close = eyes.close(true);
//            Assert.assertNotNull(close);
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

    private void initLogging(String testedUrl, Eyes eyes) {
        String testName = testedUrl.substring(8);
        String path = "." + File.separator+ "sageDebug" + File.separator;
        visualGridRunner.setDebugResourceWriter(new FileDebugResourceWriter(eyes.getLogger(), path,"", ""));
//        FileDebugResourceWriter fileDebugResourceWriter = new FileDebugResourceWriter(visualGridRunner.getLogger(), path, null, null);
//        VisualGridEyes.setDebugResourceWriter(fileDebugResourceWriter);

//        FileLogger eyesLogger = new FileLogger("TopTenSites.log", true, true);
//        eyes.setLogHandler(eyesLogger);
    }

    @AfterMethod
    public void afterMethod(ITestContext testContext) {
        visualGridRunner.getLogger().log("enter");
    }

    @AfterClass
    public void afterClass(ITestContext testContext) {
        TestResultsSummary allTestResults = visualGridRunner.getAllTestResults(false);
        for (TestResultContainer allTestResult : allTestResults) {
            System.out.println(allTestResult.toString());
        }
        visualGridRunner.getLogger().log(allTestResults.toString());
    }
}
