package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.FileLogger;
import com.applitools.eyes.ProxySettings;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.TestResultContainer;
import com.applitools.eyes.TestResultsSummary;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class TestVGExceptionInRegion {

    VisualGridRunner runner = new VisualGridRunner(3);
    private Eyes eyes;

    @Test
    public void test() {
        eyes = new Eyes(runner);
        eyes.setLogHandler(new FileLogger("VGExcep.log", false, true));
        ChromeDriver driver = new ChromeDriver();
        Configuration configuration = new Configuration();
//        configuration.setWaitBeforeScreenshots(600);
        driver.get("http://google.com");
        eyes.setConfiguration(configuration);
        eyes.open(driver, "Java SDK", "TestVGExceptionInRegion");

        SeleniumCheckSettings region = Target.window().ignore(By.xpath("//*[@id=\"hplogo\"]"));
        SeleniumCheckSettings checkSettings = region.beforeRenderScreenshotHook("let elm = document.getElementById(\"hplogo\");\n" +
                "\n" +
                "if (elm) {\n" +
                "    elm.parentNode.removeChild(elm);\n" +
                "}\n");
        eyes.check(checkSettings.fully().withName("first Step"));
        eyes.checkWindow("Second Step");
        driver.quit();
    }

    @AfterMethod
    public void after(ITestResult testContext) {
        eyes.closeAsync();
    }

    @AfterClass
    public void afterClass() {
        runner.getAllTestResults();

    }
}
