package com.applitools.eyes.utils;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class SeleniumUtils {
    public static ChromeDriver createChromeDriver(){
        ChromeOptions options = new ChromeOptions();
        if (TestUtils.runHeadless)
        {
            options.addArguments("--headless");
        }
        ChromeDriver driver = new ChromeDriver(options);
        return driver;
    }
}
