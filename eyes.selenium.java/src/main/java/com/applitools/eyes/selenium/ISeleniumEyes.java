package com.applitools.eyes.selenium;

import com.applitools.ICheckSettings;
import com.applitools.eyes.IEyesBase;
import com.applitools.eyes.TestResults;
import org.openqa.selenium.WebDriver;

public interface ISeleniumEyes extends IEyesBase {

    WebDriver open(WebDriver webDriver);

    void check(String tag, ICheckSettings checkSettings);
    void check(ICheckSettings... checkSettings);

    TestResults close(boolean throwEx);

    WebDriver getDriver();
}
