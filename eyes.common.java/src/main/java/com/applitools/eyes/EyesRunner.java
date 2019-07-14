package com.applitools.eyes;

public abstract class EyesRunner {

    protected Logger logger = new Logger();

    public abstract TestResultsSummary getAllTestResults();

    public abstract TestResultsSummary getAllTestResults(boolean shouldThrowException);

    public void setLogHandler(LogHandler logHandler) {
        logger.setLogHandler(logHandler);
        if (!logHandler.isOpen()) {
            logHandler.open();
        }
    }

    public Logger getLogger(){
      return this.logger;
    }

}
