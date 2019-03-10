package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.ProxySettings;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.config.SeleniumConfiguration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgridclient.model.RenderBrowserInfo;
import com.applitools.eyes.visualgridclient.services.VisualGridRunner;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public final class TestRenderingGridMultiThreadService {

    private VisualGridRunner renderingManager;
    private WebDriver webDriver;

    @BeforeMethod
    public void Before(ITestContext testContext) {
        renderingManager = new VisualGridRunner(1);
        renderingManager.setLogHandler(new StdoutLogHandler(true));

        webDriver = new ChromeDriver();
        webDriver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage");
        //webDriver.get("http://applitools-vg-test.surge.sh/test.html");

        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }

    @Test
    public void test() {

        final Eyes eyes = new Eyes(renderingManager);
        eyes.setBatch(new BatchInfo("VG-2ThreadBatchThreads"));

        final String baselineEnvName = "";
        Thread threadA = new Thread(new Runnable() {
            @Override
            public void run() {

                TestThreadMethod("VG-2ThreadBatchC11",
                        new RenderBrowserInfo(800, 600, SeleniumConfiguration.BrowserType.CHROME, baselineEnvName),
                        new RenderBrowserInfo(700, 500, SeleniumConfiguration.BrowserType.CHROME, baselineEnvName),
                        new RenderBrowserInfo(400, 300, SeleniumConfiguration.BrowserType.CHROME, baselineEnvName));
            }
        });

        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                TestThreadMethod("VG-2ThreadBatchC22",
                        new RenderBrowserInfo(840, 680, SeleniumConfiguration.BrowserType.CHROME, baselineEnvName),
                        new RenderBrowserInfo(750, 530, SeleniumConfiguration.BrowserType.CHROME, baselineEnvName));
            }
        });

        threadA.start();
        threadB.start();

        try {
            threadA.join();
            threadB.join();
        } catch (InterruptedException e) {
            GeneralUtils.logExceptionStackTrace(renderingManager.getLogger(), e);
        }
    }

    private void TestThreadMethod(String batchName, RenderBrowserInfo... browsersInfo) {
        Eyes eyes = new Eyes(renderingManager);
        eyes.setBatch(new BatchInfo(batchName));
        SeleniumConfiguration seleniumConfiguration = new SeleniumConfiguration();
        seleniumConfiguration.setTestName("Open Concurrency with Batch 3");
        seleniumConfiguration.setAppName("RenderingGridIntegration");
        seleniumConfiguration.addBrowsers(browsersInfo);
        eyes.setProxy(new ProxySettings("http://127.0.0.1", 8888, null, null));
        eyes.open(webDriver);
        eyes.check(Target.window().withName("test").sendDom(false));
        TestResults close = eyes.close();
        Assert.assertNotNull(close);

    }

    @AfterMethod
    public void After(ITestContext testContext) {
        renderingManager.getLogger().log(renderingManager.getAllTestResults().toString());
        webDriver.quit();
    }
}