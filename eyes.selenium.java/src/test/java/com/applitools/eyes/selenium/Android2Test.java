package com.applitools.eyes.selenium;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.Logger;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.selenium.fluent.Target;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Android2Test {

    private static BatchInfo batchInfo = new BatchInfo("Java3 Tests");
    private String  sauceUrl = "http://ondemand.saucelabs.com/wd/hub";
    private String  SAUCE_LABS_URL = "http://ondemand.saucelabs.com/wd/hub";

    @BeforeClass
    public static void classSetup() {
        String batchId = System.getenv("APPLITOOLS_BATCH_ID");
        if (batchId != null) {
            batchInfo.setId(batchId);
        }
    }

    @DataProvider(parallel = true)
    public static Object[][] data() {
        Object[][] googlePixelPermutations = TestUtils.generatePermutations(
                Arrays.asList(new Object[]{"Google Pixel GoogleAPI Emulator"}), // device
                Arrays.asList(new Object[]{"portrait", "landscape"}), // orientation
                Arrays.asList(new Object[]{"7.1"}), // OS Version
                Arrays.asList(new Object[]{false, true}) // fully
        );

        ArrayList<Object[]> returnValue = new ArrayList<>();
        returnValue.addAll(Arrays.asList(googlePixelPermutations));

        return returnValue.toArray(new Object[0][]);
    }

    @Test(dataProvider = "data")
    public void TestAndroidChromeCrop(String deviceName, String deviceOrientation, String platformVersion, boolean fully) throws MalformedURLException {
        Eyes eyes = new Eyes();

        eyes.setBatch(batchInfo);

        // This is your api key, make sure you use it in all your tests.
        DesiredCapabilities caps = DesiredCapabilities.iphone();
        caps.setCapability("deviceName", deviceName);
        caps.setCapability("deviceOrientation", deviceOrientation);
        caps.setCapability("platformVersion", platformVersion);
        caps.setCapability("platformName", "Android");
        caps.setCapability("browserName", "Chrome");

        caps.setCapability("username", System.getenv("SAUCE_USERNAME"));
        caps.setCapability("accesskey", System.getenv("SAUCE_ACCESS_KEY"));


        WebDriver driver = new RemoteWebDriver(new URL(sauceUrl), caps);
        driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);
        eyes.setLogHandler(new StdoutLogHandler(true));

        String testName = String.format("%s %s %s", deviceName, platformVersion, deviceOrientation);
        if (fully) {
            testName += " fully";
        }

        if (System.getenv("CI") == null) {
            //String logFilename = String.format("c:\\temp\\logs\\iostest_%s.log", testName);
            //eyes.setLogHandler(new FileLogger(logFilename, false, true));
            //eyes.setImageCut(new FixedCutProvider(30, 12, 8, 5));
            //eyes.setForceFullPageScreenshot(true);
            //eyes.setSaveDebugScreenshots(true);
            //eyes.setDebugScreenshotsPath("C:\\temp\\logs");
            //eyes.setDebugScreenshotsPrefix("iostest_" + testName);
        } else {
            eyes.setLogHandler(new StdoutLogHandler(true));
        }

        eyes.setStitchMode(StitchMode.SCROLL);

        eyes.addProperty("Orientation", deviceOrientation);
        eyes.addProperty("Stitched", fully ? "True" : "False");

        try {
            driver.get("https://www.applitools.com/customers");
            eyes.open(driver, "Eyes Selenium SDK - Android Chrome Cropping", testName);
            eyes.check("Initial view", Target.region(By.cssSelector("body")).fully(fully));
            eyes.close();
        } finally {
            eyes.abortIfNotClosed();
            driver.quit();
        }
    }

    @Test
    public void setUp() throws IOException {
        StdoutLogHandler LOG = new StdoutLogHandler();
        Eyes eyes = new Eyes();
        //eyes.setServerUrl(APPLITOOLS_SERVER_URL);
        // StitchMode SCROLL will fail as well (with nullpointer exception)
        eyes.setStitchMode(StitchMode.CSS);

        MutableCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("browserName", "Chrome");
        capabilities.setCapability("deviceName", "Android Emulator");
        capabilities.setCapability("deviceType", "tablet");
        capabilities.setCapability("platformName", "Android");
        capabilities.setCapability("platformVersion", "6.0");
        capabilities.setCapability("name", "Minimal test");
        //capabilities.setCapability("tunnelIdentifier", SAUCE_LABS_TUNNEL_IDENTIFIER);
        String sauce_username = System.getenv("SAUCE_USERNAME");
        capabilities.setCapability("username", sauce_username);
        String sauce_access_key = System.getenv("SAUCE_ACCESS_KEY");
        capabilities.setCapability("accesskey", sauce_access_key);

        WebDriver driver = new AppiumDriver<>(new URL(SAUCE_LABS_URL), capabilities);

        //works
        //driver = new RemoteWebDriver(new URL(new URL(SAUCE_LABS_URL), capabilities);

        driver.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);

        try {
            driver = eyes.open(driver, "NS.nl", "Minimal test");

//            LOGGER.verbose("Open url {}.", URL);
//            driver.get(URL);

            // Will fail with "java.lang.IllegalArgumentException: scrollRootElement is null"
            // at com.applitools.utils.ArgumentGuard.notNull(ArgumentGuard.java:19)
            eyes.check(Target.window().fully());

            eyes.close();
        } finally {
            eyes.abortIfNotClosed();
            driver.quit();
        }
    }


}