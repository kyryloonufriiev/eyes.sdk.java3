package com.applitools.eyes.selenium;

import com.applitools.eyes.*;

import java.util.ArrayList;
import java.util.List;

public class ClassicRunner extends EyesRunner {

    private List<TestResults> allTestResult = new ArrayList<>();

    private Error exception;

    @Override
    public TestResultsSummary getAllTestResults(){
        return getAllTestResults(false);
    }

    @Override
    public TestResultsSummary getAllTestResults(boolean shouldThrowException) {
        if (shouldThrowException && exception != null)
        {
            throw exception;
        }
        List<TestResultContainer> result = new ArrayList<>();
        for (TestResults testResults : allTestResult) {
            result.add(new TestResultContainer(testResults, null, null));
            EyesBase.logSessionResultsAndThrowException(logger, shouldThrowException, testResults);
        }
        return new TestResultsSummary(result);
    }

    void aggregateResult(TestResults testResult){
        this.allTestResult.add(testResult);
    }

    public void setException(Error exception) {
        this.exception = exception;
    }
}
