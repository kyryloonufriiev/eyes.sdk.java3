package com.applitools.eyes.visualGridClient.services;

import com.applitools.eyes.Logger;
import com.applitools.eyes.visualGridClient.model.RenderingTask;
import com.applitools.utils.GeneralUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RenderingGridService extends Thread {

    private static final int FACTOR = 5;
    private final Object debugLock;
    private final RGServiceListener listener;
    private boolean isServiceOn = true;
    private ExecutorService executor;
    protected Logger logger;


    public interface RGServiceListener {
        RenderingTask getNextTask();
    }

    RenderingGridService(String serviceName, ThreadGroup servicesGroup, Logger logger, int threadPoolSize, Object debugLock, RGServiceListener listener) {
        super(servicesGroup, serviceName);
        this.executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize * FACTOR, 1, TimeUnit.DAYS, new ArrayBlockingQueue<Runnable>(20));
        this.debugLock = debugLock;
        this.listener = listener;
        this.logger = logger;
    }

    @Override
    public void run() {
        if (debugLock != null) {
            synchronized (debugLock) {
                try {
                    debugLock.wait();
                } catch (InterruptedException e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                }
            }
        }
        while (isServiceOn) {
            runNextTask();
        }
        if (this.executor != null) {
            this.executor.shutdown();
        }
        logger.log("Service '" + this.getName() + "' is finished");
    }

    private void runNextTask() {
        final RenderingTask task = this.listener.getNextTask();
        if (task != null) {
            task.addListener(new RenderingTask.RenderTaskListener() {
                @Override
                public void onRenderSuccess() {
                    debugNotify();
                }

                @Override
                public void onRenderFailed(Exception e) {
                    debugNotify();
                }
            });
            this.executor.submit(task);
        }
    }

    private void debugNotify() {
        synchronized (debugLock) {
            debugLock.notify();
        }
    }

    public void stopService() {
        this.isServiceOn = false;
    }
}