package com.applitools.eyes.selenium;

import com.applitools.eyes.IServerConnector;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.utils.TestUtils;
import com.applitools.utils.GeneralUtils;
import com.sun.jndi.toolkit.url.Uri;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class TestMobileDevices {
    private static final String SAUCE_USERNAME = System.getenv("SAUCE_USERNAME");
    private static final String SAUCE_ACCESS_KEY = System.getenv("SAUCE_ACCESS_KEY");

    private final String page;

    @DataProvider(name = "pages")
    public static Object[][] pages() {
        return new Object[][]{{"mobile"}, {"desktop"}};
    }

    @Factory(dataProvider = "pages")
    public TestMobileDevices(String page) {
        this.page = page;
    }

    @DataProvider(name = "IOSDevices")
    public static Object[][] IOSDevices() {
        List<Object[]> devices = Arrays.asList(new Object[][]{
                {"iPad Pro (9.7 inch) Simulator", "12.0", ScreenOrientation.LANDSCAPE, false},
                {"iPhone XR Simulator", "12.2", ScreenOrientation.PORTRAIT, true}
        });
        if (TestUtils.runOnCI) {
            devices.addAll(Arrays.asList(new Object[][]{
                    {"iPad Air 2 Simulator", "12.0", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Air 2 Simulator", "11.3", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Air 2 Simulator", "11.0", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Air 2 Simulator", "10.3", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Air 2 Simulator", "12.0", ScreenOrientation.PORTRAIT, false},
                    {"iPad Air 2 Simulator", "11.3", ScreenOrientation.PORTRAIT, false},
                    {"iPad Air 2 Simulator", "11.0", ScreenOrientation.PORTRAIT, false},
                    {"iPad Air 2 Simulator", "10.3", ScreenOrientation.PORTRAIT, false},
                    {"iPad Air Simulator", "12.0", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Air Simulator", "11.0", ScreenOrientation.PORTRAIT, true},
                    {"iPad Simulator", "11.0", ScreenOrientation.LANDSCAPE, true},
                    {"iPad Simulator", "11.0", ScreenOrientation.PORTRAIT, false},
                    {"iPad (5th generation) Simulator", "11.0", ScreenOrientation.PORTRAIT, false},
                    {"iPad Pro (9.7 inch) Simulator", "11.0", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Pro (9.7 inch) Simulator", "11.0", ScreenOrientation.LANDSCAPE, true},
                    {"iPad Pro (12.9 inch) (2nd generation) Simulator", "11.0", ScreenOrientation.LANDSCAPE, false},
                    {"iPad Pro (12.9 inch) (2nd generation) Simulator", "11.0", ScreenOrientation.PORTRAIT, true},
                    {"iPad Pro (12.9 inch) (2nd generation) Simulator", "12.0", ScreenOrientation.PORTRAIT, true},
                    {"iPad Pro (10.5 inch) Simulator", "11.0", ScreenOrientation.PORTRAIT, false},
                    {"iPad Pro (10.5 inch) Simulator", "11.0", ScreenOrientation.LANDSCAPE, true},
                    {"iPhone XS Max Simulator", "12.2", ScreenOrientation.LANDSCAPE, false},
                    {"iPhone XS Max Simulator", "12.2", ScreenOrientation.LANDSCAPE, true},
                    {"iPhone XS Max Simulator", "12.2", ScreenOrientation.PORTRAIT, false},
                    {"iPhone XS Max Simulator", "12.2", ScreenOrientation.PORTRAIT, true},
                    {"iPhone XS Simulator", "12.2", ScreenOrientation.PORTRAIT, false},
                    {"iPhone XS Simulator", "12.2", ScreenOrientation.LANDSCAPE, false},
                    {"iPhone XS Simulator", "12.2", ScreenOrientation.PORTRAIT, true},
                    {"iPhone XS Simulator", "12.2", ScreenOrientation.LANDSCAPE, true},
                    {"iPhone XR Simulator", "12.2", ScreenOrientation.PORTRAIT, false},
                    {"iPhone XR Simulator", "12.2", ScreenOrientation.LANDSCAPE, false},
                    {"iPhone XR Simulator", "12.2", ScreenOrientation.LANDSCAPE, true},
                    {"iPhone X Simulator", "11.2", ScreenOrientation.PORTRAIT, false},
                    {"iPhone X Simulator", "11.2", ScreenOrientation.PORTRAIT, true},
                    {"iPhone 7 Simulator", "10.3", ScreenOrientation.PORTRAIT, true},
                    {"iPhone 6 Plus Simulator", "11.0", ScreenOrientation.PORTRAIT, false},
                    {"iPhone 6 Plus Simulator", "11.0", ScreenOrientation.LANDSCAPE, true},
                    {"iPhone 5s Simulator", "10.3", ScreenOrientation.LANDSCAPE, false},
                    {"iPhone 5s Simulator", "10.3", ScreenOrientation.LANDSCAPE, true}
            }));
        }
        return devices.toArray(new Object[0][]);
    }

    @DataProvider(name = "androidDevices")
    public static Object[][] androidDevices() {
        List<Object[]> devices = Arrays.asList(new Object[][]{
                {"Android Emulator", "8.0", ScreenOrientation.PORTRAIT, false},
                {"Android Emulator", "8.0", ScreenOrientation.LANDSCAPE, true}
        });

        return devices.toArray(new Object[0][]);
    }

    private void initEyes(String deviceName, String platformVersion, ScreenOrientation deviceOrientation, boolean fully,
                          String platformName, String browserName, String page) {
        Eyes eyes = new Eyes();

        eyes.setBatch(TestsDataProvider.batchInfo);

        DesiredCapabilities caps = new DesiredCapabilities();

        caps.setCapability("deviceName", deviceName);
        caps.setCapability("deviceOrientation", deviceOrientation.toString().toLowerCase());
        caps.setCapability("platformVersion", platformVersion);
        caps.setCapability("platformName", platformName);
        caps.setCapability("browserName", browserName);

        caps.setCapability("username", SAUCE_USERNAME);
        caps.setCapability("accesskey", SAUCE_ACCESS_KEY);

        String testName = initTestName(deviceName, platformVersion, deviceOrientation, fully, page);

        caps.setCapability("name", testName + " (" + eyes.getFullAgentId() + ")");

        String sauceUrl = "http://ondemand.saucelabs.com/wd/hub";
        WebDriver driver = null;
        try {
            driver = new RemoteWebDriver(new URL(sauceUrl), caps);
        } catch (MalformedURLException e) {
            GeneralUtils.logExceptionStackTrace(eyes.getLogger(), e);
        }
        if (driver != null) {
            driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS);

            TestUtils.setupLogging(eyes, testName);

            eyes.setStitchMode(StitchMode.CSS);

            eyes.addProperty("Orientation", deviceOrientation.toString());
            eyes.addProperty("Stitched", fully ? "True" : "False");
            runTest(fully, eyes, testName, driver, page);
        } else {
            Assert.fail("failed to initialize web driver.");
        }
    }

    private void runTest(boolean fully, Eyes eyes, String testName, WebDriver driver, String page) {
        try {
            driver.get("https://applitools.github.io/demo/TestPages/DynamicResolution/" + page + ".html");
            eyes.open(driver, "Eyes Selenium SDK - iOS Safari Cropping", testName);
            //eyes.Check("Initial view", Target.Region(By.CssSelector("div.page")).Fully(fully).SendDom(false));
            eyes.check(Target.window().fully(fully));
            TestResults result = eyes.close();
            IServerConnector serverConnector = eyes.getServerConnector();
            SessionId session = ((RemoteWebDriver) driver).getSessionId();
            serverConnector.putTestResultJsonToSauce(new PassedResult(result.isPassed()), session.toString());
        } finally {
            eyes.abort();
            driver.quit();
        }
    }

    @Test(dataProvider = "IOSDevices")
    public void TestIOSSafariCrop_SauceLabs(String deviceName, String platformVersion, ScreenOrientation deviceOrientation, boolean fully) {
        initEyes(deviceName, platformVersion, deviceOrientation, fully, "iOS", "Safari", this.page);
    }

    @Test(dataProvider = "androidDevices")
    public void TestAndroid_SauceLabs(String deviceName, String platformVersion, ScreenOrientation deviceOrientation, boolean fully) {
        initEyes(deviceName, platformVersion, deviceOrientation, fully, "Android", "Chrome", page);
    }

    private String initTestName(String deviceName, String platformVersion, ScreenOrientation deviceOrientation, boolean fully, String page) {
        String testName = deviceName + " " + platformVersion + " " + deviceOrientation + " " + page;

        if (fully) {
            testName += " fully";
        }

        return testName;
    }

}
