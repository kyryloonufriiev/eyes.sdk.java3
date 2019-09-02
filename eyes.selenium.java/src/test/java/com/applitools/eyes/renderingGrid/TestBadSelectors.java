package com.applitools.eyes.renderingGrid;

import com.applitools.eyes.RectangleSize;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestBadSelectors {
    VisualGridRunner runner = new VisualGridRunner(5);
    @DataProvider(name="booleanDP")
    public Object[] dp() {
        return new Object[]{Boolean.TRUE, Boolean.FALSE};
    }

    @Test(dataProvider = "booleanDP")
    public void TestCheckRegionWithBadSelector(boolean useVisualGrid)
    {
        final WebDriver driver = new ChromeDriver();
        final Eyes eyes = new Eyes();
        driver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage/");

        String suffix = useVisualGrid ? "_VG" : "";
        eyes.open(driver, "Applitools Eyes Java SDK", "TestCheckRegionWithBadSelector" + suffix, new RectangleSize(1200, 800));

        Throwable ex = Assert.expectThrows(NoSuchElementException.class, new Assert.ThrowingRunnable() {
            @Override
            public void run(){
                eyes.checkRegion(By.cssSelector("#element_that_does_not_exist"));
                eyes.closeAsync();
                runner.getAllTestResults();
                driver.quit();
            }
        });
    }

    @Test(dataProvider = "booleanDP")
    public void TestCheckRegionWithBadIgnoreSelector(boolean useVisualGrid)
    {
        final WebDriver driver = new ChromeDriver();
        final Eyes eyes = new Eyes();
        driver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage/");

        String suffix = useVisualGrid ? "_VG" : "";
        eyes.open(driver, "Applitools Eyes DotNet SDK", "TestCheckRegionWithBadIgnoreSelector" + suffix, new RectangleSize(1200, 800));

        eyes.check(Target.window().ignore(By.cssSelector("body>p:nth-of-type(14)"))
                .beforeRenderScreenshotHook("var p = document.querySelector('body>p:nth-of-type(14)'); p.parentNode.removeChild(p);"));

        eyes.close();
        runner.getAllTestResults();
        driver.quit();
    }

    @Test(dataProvider = "booleanDP")
    public void TestCheckRegionWithBadSelectorBeforeValidCheck(boolean useVisualGrid) {
        final WebDriver driver = new ChromeDriver();
        final Eyes eyes = new Eyes();
        driver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage/");

        String suffix = useVisualGrid ? "_VG" : "";
        eyes.open(driver, "Applitools Eyes DotNet SDK", "TestCheckRegionWithBadSelectorBeforeValidCheck" + suffix, new RectangleSize(1200, 800));

        Throwable ex = Assert.expectThrows(NoSuchElementException.class, new Assert.ThrowingRunnable() {
            @Override
            public void run() {
                eyes.checkRegion(By.cssSelector("#element_that_does_not_exist"));
                driver.findElement(By.id("centered")).click();
                eyes.checkRegion(By.id("modal-content"));

                eyes.closeAsync();
                runner.getAllTestResults();
                driver.quit();
            }
        });
    }
}
