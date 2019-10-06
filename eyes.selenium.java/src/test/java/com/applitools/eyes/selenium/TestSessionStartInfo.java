package com.applitools.eyes.selenium;

import com.applitools.AccessibilityLevel;
import com.applitools.eyes.*;
import com.applitools.eyes.selenium.fluent.Target;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Calendar;

@Listeners(TestListener.class)
@SuppressWarnings("SpellCheckingInspection")
public class TestSessionStartInfo {


    @Test()
    public void TestSessionInfo() {
        Calendar instance = Calendar.getInstance();
        instance.set(2017, 6, 29, 11, 1, 0);
        BatchInfo batchInfo = new BatchInfo("some batch", instance);
        batchInfo.setId("someBatchId");
        ImageMatchSettings ims = new ImageMatchSettings();
        ims.setMatchLevel(MatchLevel.STRICT);
        AccessibilityRegionByRectangle[] accessibilityValidation = {new AccessibilityRegionByRectangle(10, 20, 30, 40, AccessibilityRegionType.GraphicalObject)};
        ims.setAccessibility(accessibilityValidation);
        ims.setAccessibilityLevel(AccessibilityLevel.AA);
        ims.setFloatingRegions(new FloatingMatchSettings[]{new FloatingMatchSettings(22, 32, 42, 52, 5, 10, 15, 20)});

        SessionStartInfo sessionStartInfo = new SessionStartInfo(
                "agent", SessionType.SEQUENTIAL,
                "some app",
                "1.0",
                "some test",
                batchInfo,
                "baseline", "some environment",
                new AppEnvironment("windows", "test suite", new RectangleSize(234, 456)),
                ims, "some branch",
                "parent branch",
                "baseline branch",
                null,
                null);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String targetJsonObj = mapper.writeValueAsString(sessionStartInfo);
            String sourceJsonAsString = GeneralUtils.readToEnd(TestDomCapture.class.getResourceAsStream("/sessionStartInfo.json"));
            boolean equals = targetJsonObj.equals(sourceJsonAsString);
            Assert.assertTrue(equals, "JSON strings are different.");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
