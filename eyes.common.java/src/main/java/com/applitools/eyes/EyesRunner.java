package com.applitools.eyes;

import java.util.HashMap;
import java.util.Map;

public abstract class EyesRunner {

    protected Logger logger = new Logger();

    private Map<String, IBatchCloser> batchesServerConnectorsMap = new HashMap<>();

    public abstract TestResultsSummary getAllTestResultsImpl();

    public abstract TestResultsSummary getAllTestResultsImpl(boolean shouldThrowException);

    public TestResultsSummary getAllTestResults() {
        return getAllTestResults(true);
    }

    public TestResultsSummary getAllTestResults(boolean shouldThrowException) {
        TestResultsSummary allTestResults;
        try {
            allTestResults = getAllTestResultsImpl(shouldThrowException);
        } finally {
            deleteAllBatches();
        }
        return allTestResults;
    }

    private void deleteAllBatches() {
        for (String batch : batchesServerConnectorsMap.keySet()) {
            IBatchCloser connector = batchesServerConnectorsMap.get(batch);
            connector.closeBatch(batch);
        }
    }

    public void setLogHandler(LogHandler logHandler) {
        logger.setLogHandler(logHandler);
        if (!logHandler.isOpen()) {
            logHandler.open();
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void addBatch(String batchId, IBatchCloser batchCloser) {
        if (!batchesServerConnectorsMap.containsKey(batchId)) {
            batchesServerConnectorsMap.put(batchId, batchCloser);
        }
    }

}
