package com.applitools.eyes.selenium;
import com.applitools.eyes.utils.CommUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.testng.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SendTestResultsListener implements ISuiteListener {

    public static AtomicReference<ArrayList<ITestResult>> allTestResults = new AtomicReference<>();
    private static AtomicReference<String> suiteId = new AtomicReference<>();

    @Override
    public void onStart(ISuite iSuite) {
        allTestResults.set(new ArrayList<ITestResult>());
        suiteId.set("123");
    }

    @Override
    public void onFinish(ISuite iSuite) {
        JsonObject resultJson = getResultJson();
        try {
            CommUtils.postJson("http://sdk-test-results.herokuapp.com/result", new Gson().fromJson(resultJson, Map.class), null);
        } catch (Throwable t) {
            CommUtils.postJson("http://sdk-test-results.herokuapp.com/result", new Gson().fromJson(resultJson, Map.class), null);
        }
    }

    private JsonObject getResultJson(){
        JsonArray resultsJsonArray = new JsonArray();
        for (ITestResult testResult: allTestResults.get()) {
            JsonObject innerResultsJsonObject = new JsonObject();
            innerResultsJsonObject.addProperty("test_name", testResult.getMethod().getMethodName());
            innerResultsJsonObject.addProperty("passed", testResult.isSuccess());
            resultsJsonArray.add(innerResultsJsonObject);
        }
        JsonObject finalJsonObject = new JsonObject();
        finalJsonObject.addProperty("sdk", "java");
        finalJsonObject.addProperty("id", suiteId.get());
        finalJsonObject.add("results", resultsJsonArray);
        return finalJsonObject;
    }
}
