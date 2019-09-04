package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.TestDataProvider;
import com.applitools.eyes.utils.SeleniumUtils;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestVGServerConfigs {
    @Test
    public void TestVGDoubleCloseNoCheck() {
        WebDriver driver = SeleniumUtils.createChromeDriver();
        try {
            VisualGridRunner runner = new VisualGridRunner(10);
            final Eyes eyes = new Eyes(runner);
            Configuration conf = new Configuration();
            conf.setAppName("app").setTestName("test");
            conf.setBatch(TestDataProvider.batchInfo);
            eyes.setConfiguration(conf);

            eyes.open(driver);
            Error ex = Assert.expectThrows(Error.class, new Assert.ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    eyes.close();
                }
            });
            Assert.assertEquals(ex.getMessage(), "java.lang.IllegalStateException: Eyes not open");
        } finally {
            driver.quit();
        }
    }
}
