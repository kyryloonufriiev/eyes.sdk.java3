package com.applitools.eyes.selenium;

import com.applitools.eyes.*;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.ITest;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

public abstract class TestSetup implements ITest {

    private Eyes seleniumEyes;

    private Eyes eyes;
    protected WebDriver driver;
    protected RemoteWebDriver webDriver;

    protected String testSuitName;

    protected String testedPageUrl = "http://applitools.github.io/demo/TestPages/FramesTestPage/";
    //protected RectangleSize testedPageSize = new RectangleSize(1200, 800);
    protected RectangleSize testedPageSize = new RectangleSize(700, 460);

    private String logsPath = System.getenv("APPLITOOLS_LOGS_PATH");

    protected Capabilities caps;
    private DesiredCapabilities desiredCaps = new DesiredCapabilities();

    protected HashSet<FloatingMatchSettings> expectedFloatingRegions = new HashSet<>();
    protected HashSet<Region> expectedIgnoreRegions = new HashSet<>();
    protected HashSet<Region> expectedLayoutRegions = new HashSet<>();
    protected HashSet<Region> expectedStrictRegions = new HashSet<>();
    protected HashSet<Region> expectedContentRegions = new HashSet<>();

    protected boolean compareExpectedRegions = false;

    protected String platform;
    protected boolean forceFPS;

    private String testName;

    @BeforeClass(alwaysRun = true)
    public void OneTimeSetUp() {

        // Initialize the seleniumEyes SDK and set your private API key.
        seleniumEyes = new Eyes();
        //seleniumEyes.setServerConnector(new ServerConnector());

//        RemoteSessionEventHandler remoteSessionEventHandler = new RemoteSessionEventHandler(
//                seleniumEyes.getLogger(), URI.create("http://localhost:3000/"), "MyAccessKey");
//        remoteSessionEventHandler.setThrowExceptions(false);
//        seleniumEyes.addSessionEventHandler(remoteSessionEventHandler);

        LogHandler logHandler = new StdoutLogHandler(false);

        seleniumEyes.setLogHandler(logHandler);
        seleniumEyes.setStitchMode(StitchMode.CSS);

        seleniumEyes.setHideScrollbars(true);

//        seleniumEyes.setProxy(new ProxySettings("http://127.0.0.1", 8888));

        String batchId = System.getenv("APPLITOOLS_BATCH_ID");
        if (batchId != null) {
            TestsDataProvider.batchInfo.setId(batchId);
        }

        seleniumEyes.setBatch(TestsDataProvider.batchInfo);
        this.setEyes(seleniumEyes);
    }

    protected void setEyes(Eyes eyes) {
        this.eyes = eyes;
    }

    public Eyes getEyes(){
        return this.eyes;
    }

    protected void setExpectedIgnoreRegions(Region... expectedIgnoreRegions) {
        this.expectedIgnoreRegions = new HashSet<>(Arrays.asList(expectedIgnoreRegions));
    }

    protected void setExpectedLayoutRegions(Region... expectedLayoutRegions) {
        this.expectedLayoutRegions = new HashSet<>(Arrays.asList(expectedLayoutRegions));
    }

    protected void setExpectedStrictRegions(Region... expectedStrictRegions) {
        this.expectedStrictRegions = new HashSet<>(Arrays.asList(expectedStrictRegions));
    }

    protected void setExpectedContentRegions(Region... expectedContentRegions) {
        this.expectedContentRegions = new HashSet<>(Arrays.asList(expectedContentRegions));
    }

    protected void setExpectedFloatingsRegions(FloatingMatchSettings... expectedFloatingsRegions) {
        this.expectedFloatingRegions = new HashSet<>(Arrays.asList(expectedFloatingsRegions));
    }

    public void beforeMethod(String methodName) {
        System.out.println();
        System.out.println("==== Starting Test ====");
        System.out.println(this);
        System.out.println();

        String fps = forceFPS ? "_FPS" : "";
        String testName = methodName + fps;
        testName = testName.replace('[', '_')
                .replace(' ', '_')
                .replace("]", "");

        String seleniumServerUrl = System.getenv("SELENIUM_SERVER_URL");
        if ("http://ondemand.saucelabs.com/wd/hub".equalsIgnoreCase(seleniumServerUrl)) {
            desiredCaps.setCapability("username", System.getenv("SAUCE_USERNAME"));
            desiredCaps.setCapability("accesskey", System.getenv("SAUCE_ACCESS_KEY"));
            //desiredCaps.setCapability("seleniumVersion", "3.11.0");

            if (caps.getBrowserName().equals("chrome")) {
                desiredCaps.setCapability("chromedriverVersion", "2.37");
            }

            desiredCaps.setCapability("platform", platform);
            desiredCaps.setCapability("name", testName + " (" + seleniumEyes.getFullAgentId() + ")");

        } else if ("http://hub-cloud.browserstack.com/wd/hub".equalsIgnoreCase(seleniumServerUrl)) {
            seleniumServerUrl = "http://" + System.getenv("BROWSERSTACK_USERNAME") + ":" + System.getenv("BROWSERSTACK_ACCESS_KEY") + "@hub-cloud.browserstack.com/wd/hub";
            desiredCaps.setCapability("platform", platform);
            desiredCaps.setCapability("name", testName + " (" + seleniumEyes.getFullAgentId() + ")");
        }

        caps.merge(desiredCaps);

        this.testName = testName + " " + caps.getBrowserName() + " " + platform;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS");

        String extendedTestName =
                testName + "_" +
                caps.getBrowserName() + "_" +
                platform + "_" +
                dateFormat.format(Calendar.getInstance().getTime());

        if (seleniumServerUrl != null) {
            try {
                webDriver = new RemoteWebDriver(new URL(seleniumServerUrl), caps);
            } catch (MalformedURLException ignored) {
            }
        } else {
            switch (caps.getBrowserName()){
                case "chrome": webDriver = new ChromeDriver((ChromeOptions)caps); break;
                default: return;
            }
        }

        LogHandler logHandler;

        if (System.getenv("CI") == null && logsPath != null) {
            String path = logsPath + File.separator + "java" + File.separator + extendedTestName.replaceAll("\\s","_");
            logHandler = new FileLogger(path + File.separator + testName + "_" + platform + ".log", true, true);
            seleniumEyes.setDebugScreenshotsPath(path);
            seleniumEyes.setDebugScreenshotsPrefix(testName + "_");
            seleniumEyes.setSaveDebugScreenshots(true);
        } else {
            logHandler = new StdoutLogHandler(false);
        }

        seleniumEyes.setLogHandler(logHandler);
        seleniumEyes.clearProperties();
        seleniumEyes.addProperty("Selenium Session ID", webDriver.getSessionId().toString());
        seleniumEyes.addProperty("ForceFPS", forceFPS ? "true" : "false");
        seleniumEyes.addProperty("ScaleRatio", "" + seleniumEyes.getScaleRatio());
        seleniumEyes.addProperty("Agent ID", seleniumEyes.getFullAgentId());
        try {
            driver = seleniumEyes.open(webDriver,
                    testSuitName,
                    testName,
                    testedPageSize
            );

            if (testedPageUrl != null) {
                driver.get(testedPageUrl);
            }

            seleniumEyes.setForceFullPageScreenshot(forceFPS);

            this.expectedIgnoreRegions.clear();
            this.expectedLayoutRegions.clear();
            this.expectedStrictRegions.clear();
            this.expectedContentRegions.clear();
            this.expectedFloatingRegions.clear();
        } catch (Exception ex) {
            seleniumEyes.abortIfNotClosed();
            webDriver.quit();
        }
    }

    @Override
    public String getTestName() {
        return testName;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s, force FPS: %s)",
                this.getClass().getSimpleName(),
                this.caps.getBrowserName(),
                this.platform,
                this.forceFPS);
    }
}
