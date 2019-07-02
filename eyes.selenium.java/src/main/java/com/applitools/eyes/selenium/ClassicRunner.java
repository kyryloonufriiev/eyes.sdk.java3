package com.applitools.eyes.selenium;

import com.applitools.eyes.TestResults;
import com.applitools.eyes.TestResultContainer;
import com.applitools.eyes.TestResultSummary;
import com.applitools.eyes.EyesRunner;

import java.util.ArrayList;
import java.util.List;

public class ClassicRunner extends EyesRunner {

    private List<TestResults> allTestResult = new ArrayList<>();

    @Override
    public TestResultSummary getAllTestResults(){
        return getAllTestResults(false);
    }

    @Override
    public TestResultSummary getAllTestResults(boolean shouldThrowException) {
        List<TestResultContainer> result = new ArrayList<>();
        for (TestResults testResults : allTestResult) {
            result.add(new TestResultContainer(testResults, null, null));
        }
        return new TestResultSummary(result);
    }

    void aggregateResult(TestResults testResult){
        this.allTestResult.add(testResult);
    }
}
