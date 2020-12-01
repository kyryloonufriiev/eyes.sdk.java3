package com.applitools.eyes.visualgrid.services;

import com.applitools.eyes.IBatchCloser;
import com.applitools.eyes.Logger;
import com.applitools.eyes.TestResultContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IRenderingEyes {

    boolean isEyesClosed();

    Logger getLogger();

    IBatchCloser getBatchCloser();

    String getBatchId();

    Map<String, RunningTest> getAllRunningTests();

    List<TestResultContainer> getAllTestResults();

    boolean isCompleted();
}
