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
        AccessibilitySettings settings = new AccessibilitySettings(AccessibilityLevel.AA, AccessibilityGuidelinesVersion.WCAG_2_0);
        Configuration configuration = new Configuration();
        configuration.setAccessibilityValidation(settings);
        eyes.setConfiguration(configuration);
        WebDriver driver = SeleniumUtils.createChromeDriver();
        try {
            driver.get("https://applitools.github.io/demo/TestPages/VisualGridTestPage/");
            eyes.open(driver, "Applitools Eyes SDK", "TestAccessibility_Sanity" + suffix, new RectangleSize(1200, 800));
            eyes.checkWindow("Sanity");
            eyes.closeAsync();
            configuration.setAccessibilityValidation(null);
            eyes.setConfiguration(configuration);
            eyes.open(driver, "Applitools Eyes SDK", "TestAccessibility_No_Accessibility" + suffix, new RectangleSize(1200, 800));
            eyes.checkWindow("No accessibility");
            eyes.closeAsync();
        } finally {
            driver.quit();
            eyes.abortAsync();
            TestResultsSummary allTestResults = runner.getAllTestResults(false);

            Assert.assertEquals(2, allTestResults.getAllResults().length);

            TestResults results1 = allTestResults.getAllResults()[0].getTestResults();
            TestResults results2 = allTestResults.getAllResults()[1].getTestResults();
            SessionAccessibilityStatus accessibilityStatus = results1.getAccessibilityStatus();
            if (accessibilityStatus == null) {
                // The order of the results isn't guaranteed when running with visual grid
                accessibilityStatus = results2.getAccessibilityStatus();
                results2 = results1;
            }
            Assert.assertEquals(accessibilityStatus.getSettings().getLevel(), AccessibilityLevel.AA);
            Assert.assertEquals(accessibilityStatus.getSettings().getGuidelinesVersion(), AccessibilityGuidelinesVersion.WCAG_2_0);
            Assert.assertNull(results2.getAccessibilityStatus());
        }
    }
}
