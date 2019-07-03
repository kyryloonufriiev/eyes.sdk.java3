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

public class TestRenderingGridServiceVans {
    private VisualGridRunner renderingManager;

    private String logsPath = System.getenv("APPLITOOLS_LOGS_PATH");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");
    private String dateTimeString = dateFormat.format(Calendar.getInstance().getTime());

    @BeforeClass
    public void beforeClass() {
        renderingManager = new VisualGridRunner(40);
        renderingManager.setLogHandler(new StdoutLogHandler(true));
        FileLogger logHandler = new FileLogger("eyes.log", false, true);
        renderingManager.setLogHandler(logHandler);
        renderingManager.getLogger().log("enter");
        renderingManager.setServerUrl("https://eyes.applitools.com/");
    }


    @DataProvider(name = "Kids",parallel=true)
    public Object[][] Kids_urls() {
        BatchInfo batch = new BatchInfo("Kids Gallery Vans - with Emulation");
        return new Object[][]{
                {"https://www.vans.com/shop/infant-baby-shoes",batch},
                {"https://www.vans.com/shop/kids-toddler-baby-shoes",batch},
                {"https://www.vans.com/shop/kids-little-boys-shoes",batch}};

    }

    @DataProvider(name = "Women",parallel=true)
    public Object[][] Women_urls() {
        BatchInfo batch = new BatchInfo("Women Gallery Vans");
        return new Object[][]{
                {"https://www.vans.com/shop/womens-shoes-new-arrivals",batch},
                {"https://www.vans.com/shop/womens-shoes-sandals",batch},
                {"https://www.vans.com/shop/shoes-classics-womens-authentics?ref=womens-styles",batch},
                {"https://www.vans.com/shop/womens-clothes-new-arrivals",batch},
                {"https://www.vans.com/shop/womens-clothes-tees",batch},
                {"https://www.vans.com/shop/womens-clothes-pants",batch},
                {"https://www.vans.com/shop/womens-shoes-classics",batch}};
    }

    @DataProvider(name = "Men",parallel=true)
    public Object[][] Men_urls() {
        BatchInfo batch = new BatchInfo("Men Gallery Vans");

        return new Object[][]{
                {"https://www.vans.com/shop/mens-shoes-skate",batch},
                {"https://www.vans.com/shop/mens-shoes-shop-new-arrivals",batch},
                {"https://www.vans.com/shop/mens-shoes",batch},
                {"https://www.vans.com/shop/mens-shoes-sandals",batch}};
    }




    @DataProvider(name = "dp", parallel = true)
    public static Object[][] dp() {
        return new Object[][]{
                {"https://google.com"},
                {"https://facebook.com"},
//                {"https://youtube.com"},
//                {"https://amazon.com"},
//                {"https://yahoo.com"},
//                {"https://ebay.com"},
//                {"https://twitter.com"},
//                {"https://wikipedia.org"},
//                {"https://instagram.com"},
//                {"https://reddit.com"},
        };
    }

    @Test(dataProvider = "Kids")
    public void test(String testedUrl, BatchInfo batch) {
        renderingManager.getLogger().log("entering with url " + testedUrl);
        WebDriver webDriver = new ChromeDriver();
        webDriver.get(testedUrl);
        Eyes eyes = initEyes(webDriver, testedUrl, batch);

        Logger logger = eyes.getLogger();
        logger.log("navigated to " + testedUrl);


        try {
            //CheckRGSettings setting = new CheckRGSettings(CheckRGSettings.SizeMode.FULL_PAGE, null, null, false);
            logger.log("running check for url " + testedUrl);
            try {
                eyes.check(Target.window().withName("Step1 - " + testedUrl).sendDom(false));
//                VisualGridEyes.check(Target.window().fully(false).withName("Step2 - " + testedUrl).sendDom(false));
            } catch (Exception e) {
                e.printStackTrace();
            }
            eyes.getLogger().log("calling VisualGridEyes.close() for url " + testedUrl);

            eyes.close();

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

    private Eyes initEyes(WebDriver webDriver, String testedUrl, BatchInfo batch) {
        Eyes eyes = new Eyes(renderingManager);
        eyes.setBatch(batch);
        initLogging(testedUrl, eyes);

        Logger logger = eyes.getLogger();
        logger.log("creating WebDriver: " + testedUrl);

        try {
            Configuration configuration = new Configuration();
            configuration.setTestName("Vans Gallery page - with ");
            configuration.setAppName("RenderingGridIntegration");
            configuration.addBrowser(800, 600, BrowserType.CHROME);
            configuration.addBrowser(1600, 1200, BrowserType.CHROME);
            configuration.addDeviceEmulation(DeviceName.iPhone6_7_8_Plus, ScreenOrientation.LANDSCAPE);
            logger.log("created configurations for url " + testedUrl);
            eyes.setConfiguration(configuration);
            eyes.open(webDriver);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return eyes;
    }

    private void initLogging(String testedUrl, Eyes eyes) {
        String testName = testedUrl.substring(8);
        String path = logsPath + File.separator + "java" + File.separator + "TestTopSites_" + dateTimeString;
//        FileDebugResourceWriter fileDebugResourceWriter = new FileDebugResourceWriter(renderingManager.getLogger(), path, null, null);
//        VisualGridEyes.setDebugResourceWriter(fileDebugResourceWriter);

        FileLogger eyesLogger = new FileLogger("TopTenSites.log", true, true);
        renderingManager.setLogHandler(eyesLogger);
    }

    @AfterMethod
    public void afterMethod(ITestContext testContext) {
        renderingManager.getLogger().log("enter");
    }

    @AfterClass
    public void afterTestRun(ITestContext testContext) {
        TestResultsSummary allTestResults = renderingManager.getAllTestResults();
        renderingManager.getLogger().log(allTestResults.toString());
    }
}