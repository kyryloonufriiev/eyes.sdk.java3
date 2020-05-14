package com.applitools.eyes.selenium;

import com.applitools.eyes.*;
import com.applitools.eyes.utils.SeleniumUtils;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestAccessibility {
    @DataProvider(name = "booleanDP")
    public Object[] dp() {
        return new Object[]{Boolean.TRUE, Boolean.FALSE};
    }

    @Test(dataProvider = "booleanDP")
    public void testSanity(boolean useVisualGrid) {
        EyesRunner runner = useVisualGrid ? new VisualGridRunner(10) : new ClassicRunner();
        String suffix = useVisualGrid ? "_VG" : "";
        Eyes eyes = new Eyes(runner);
        eyes.setApiKey("D98LyaCRbaPoEDpIyF99AKiUHAzx1JUoqITFiyF104mHniE110");
        eyes.setServerUrl("https://testeyesapi.applitools.com");
        eyes.setProxy(new ProxySettings("http://localhost:8888"));
        AccessibilitySettings settings = new AccessibilitySettings(AccessibilityLevel.AA, AccessibilityGuidelinesVersion.WCAG_2_0);
        Configuration configuration = new Configuration();
        configuration.setAccessibilityValidation(settings);
        eyes.setConfiguration(configuration);
        WebDriver driver = SeleniumUtils.createChromeDriver();
        try {
            driver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage/");
            eyes.open(driver, "Applitools Eyes SDK", "TestAccessibility" + suffix, new RectangleSize(1200, 800));
            eyes.checkWindow("Sanity");
            configuration.setAccessibilityValidation(null);
            eyes.checkWindow("No accessibility");
            eyes.closeAsync();
        } finally {
            driver.quit();
            eyes.abortAsync();
            TestResultsSummary allTestResults = runner.getAllTestResults(false);
            Assert.assertEquals(2, allTestResults.getAllResults().length);
            TestResults results1 = allTestResults.getAllResults()[0].getTestResults();
            Assert.assertEquals(results1.getAccessibilityStatus().getStatus(), SessionAccessibilityStatus.AccessibilityStatus.PASSED);
            Assert.assertEquals(results1.getAccessibilityStatus().getSettings(), settings);
            TestResults results2 = allTestResults.getAllResults()[1].getTestResults();
            Assert.assertEquals(results2.getAccessibilityStatus().getStatus(), SessionAccessibilityStatus.AccessibilityStatus.PASSED);
            Assert.assertNull(results2.getAccessibilityStatus().getSettings());
        }
    }
}
