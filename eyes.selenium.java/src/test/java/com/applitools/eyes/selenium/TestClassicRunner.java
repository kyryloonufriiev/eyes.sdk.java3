package com.applitools.eyes.selenium;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.ProxySettings;
import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.StdoutLogHandler;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.model.TestResultSummary;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Test;

public final class TestClassicRunner {
    @Test
    public void test(){

        // Open a Chrome browser.
        WebDriver driver = new ChromeDriver();

        // Initialize the VisualGridEyes SDK and set your private API key.
        ClassicRunner runner = new ClassicRunner();
        Eyes eyes = new Eyes(runner);

        eyes.setLogHandler(new StdoutLogHandler(true));
        eyes.setServerUrl("https://eyes.applitools.com/");
//        eyes.setProxy(new ProxySettings("http://127.0.0.1:8888"));

        // Switch sendDom flag on
        eyes.setSendDom(true);
        eyes.setStitchMode(StitchMode.CSS);
        BatchInfo batchInfo = new BatchInfo("TTS - config batch");
        batchInfo.setId("RCA_Batch_ID");
        eyes.setBatch(batchInfo);
//        try {



        // Navigate the browser to the "hello world!" web-site.
        driver.get("http://applitools.github.io/demo/TestPages/VisualGridTestPage/index.html");

        eyes.open(driver, "Applitools Eyes Java SDK", "Classic Runner Test",
                new RectangleSize(1200, 800));


        eyes.check(Target.window().fully().ignoreDisplacements(false).withName("Step 1"));



        eyes.close(true);

        eyes.open(driver, "Applitools Eyes Java SDK", "Classic Runner Test",
                new RectangleSize(1200, 800));

        eyes.check(Target.window().fully().ignoreDisplacements(false).withName("Step 1"));

        eyes.close(true);

        driver.quit();

        TestResultSummary allTestResults = runner.getAllTestResults();
        if (allTestResults.getAllResults().length != 2){
            throw new Error();
        }
    }
}