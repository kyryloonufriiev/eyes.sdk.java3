package com.applitools.eyes;

import com.applitools.eyes.LogHandler;
import com.applitools.eyes.Logger;
import com.applitools.eyes.visualgrid.model.TestResultSummary;

public abstract class EyesRunner {

    protected Logger logger = new Logger();

    public abstract TestResultSummary getAllTestResults();

    public abstract TestResultSummary getAllTestResults(boolean shouldThrowException);

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
