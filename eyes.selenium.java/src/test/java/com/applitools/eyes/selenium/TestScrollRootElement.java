package com.applitools.eyes.selenium;

import com.applitools.eyes.selenium.fluent.Target;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.testng.annotations.Factory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(TestListener.class)
public class TestScrollRootElement extends TestSetup {

    @Factory(dataProvider = "dp", dataProviderClass = TestsDataProvider.class)
    public TestScrollRootElement(Capabilities caps, String mode) {
        super("SeleniumEyes Selenium SDK - Scroll Root Element", caps, mode);
        testedPageUrl = "https://applitools.github.io/demo/TestPages/SimpleTestPage/scrollablebody.html";
    }

    @Test
    public void TestCheckWindow_Body_Scroll() {
        getEyes().setStitchMode(StitchMode.SCROLL);
        getEyes().check("Body (Scroll stitching)", Target.window().scrollRootElement(By.tagName("body")).fully());
    }

    @Test
    public void TestCheckWindow_Html_Scroll() {
        getEyes().setStitchMode(StitchMode.SCROLL);
        getEyes().check("Html (Scroll stitching)", Target.window().scrollRootElement(By.tagName("html")).fully());
    }

    @Test
    public void TestCheckWindow_Body_Css() {
        getEyes().setStitchMode(StitchMode.CSS);
        getEyes().check("Body (Css stitching)", Target.window().scrollRootElement(By.tagName("body")).fully());
    }

    @Test
    public void TestCheckWindow_Html_Css() {
        getEyes().setStitchMode(StitchMode.CSS);
        getEyes().check("Html (Css stitching)", Target.window().scrollRootElement(By.tagName("html")).fully());
    }
}
