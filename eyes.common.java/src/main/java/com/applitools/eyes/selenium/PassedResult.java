package com.applitools.eyes.selenium;

public class PassedResult {
    boolean isPassedResult = false;

    public PassedResult(boolean isPassedResult) {
        this.isPassedResult = isPassedResult;
    }

    public boolean isPassedResult() {
        return isPassedResult;
    }

    public void setPassedResult(boolean passedResult) {
        isPassedResult = passedResult;
    }
}
