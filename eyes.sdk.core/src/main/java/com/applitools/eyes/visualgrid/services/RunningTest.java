package com.applitools.eyes.visualgrid.services;


import com.applitools.ICheckSettings;
import com.applitools.eyes.IBatchCloser;
import com.applitools.eyes.Logger;
import com.applitools.eyes.TestResultContainer;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.config.ConfigurationProvider;
import com.applitools.eyes.visualgrid.model.EyesConnector;
import com.applitools.eyes.visualgrid.model.RenderBrowserInfo;
import com.applitools.eyes.visualgrid.model.VisualGridSelector;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunningTest {
    // The maximum number of steps which can run in parallel
    static final int PARALLEL_STEPS_LIMIT = 1;

    private final List<VisualGridTask> visualGridTaskList = Collections.synchronizedList(new ArrayList<VisualGridTask>());
    private IRenderingEyes eyes;
    private EyesConnector eyesConnector;
    private final RenderBrowserInfo browserInfo;
    private final AtomicBoolean isTestOpen = new AtomicBoolean(false);
    private final AtomicBoolean isTestClose = new AtomicBoolean(false);
    private final AtomicBoolean isTestInExceptionMode = new AtomicBoolean(false);
    private IRenderingEyes.EyesListener listener;
    private ConfigurationProvider configurationProvider;
    private final HashMap<VisualGridTask, FutureTask<TestResultContainer>> taskToFutureMapping = new HashMap<>();
    private final Logger logger;
    private final AtomicBoolean isCloseTaskIssued = new AtomicBoolean(false);
    private VisualGridTask closeTask;
    private VisualGridTask openTask;
    private String appName;
    private String testName;

    private final VisualGridTask.VGTaskListener taskListener = new VisualGridTask.VGTaskListener() {
        @Override
        public void onTaskComplete(VisualGridTask visualGridTask) {
            logger.verbose("locking runningTest.visualGridTaskList");
            synchronized (visualGridTaskList) {
                visualGridTaskList.remove(visualGridTask);
            }
            logger.verbose("releasing runningTest.visualGridTaskList");
            switch (visualGridTask.getType()) {
                case OPEN:
                    isTestOpen.set(true);
                    break;
                case CLOSE:
                case ABORT:
                    isTestClose.set(true);
                    break;
            }
            if (listener != null) {
                listener.onTaskComplete(visualGridTask, eyes);
            }
        }

        @Override
        public void onTaskFailed(Throwable e, VisualGridTask visualGridTask) {
            synchronized (visualGridTaskList) {
                visualGridTaskList.remove(visualGridTask);
            }
            setTestInExceptionMode(e);
            listener.onTaskComplete(visualGridTask, eyes);
        }

        @Override
        public void onRenderComplete() {
            logger.verbose("enter");
            listener.onRenderComplete();
            logger.verbose("exit");
        }

    };

    /******** BEGIN - PUBLIC FOR TESTING PURPOSES ONLY ********/
    public void setCloseTask(VisualGridTask task) {
        this.closeTask = task;
    }

    public VisualGridTask getCloseTask() {
        return this.closeTask;
    }

    public void setOpenTask(VisualGridTask task) {
        this.openTask = task;
    }

    public RunningTest(RenderBrowserInfo browserInfo, Logger logger)
    {
        this.browserInfo = browserInfo;
        this.logger = logger;
    }

    public RunningTest(RenderBrowserInfo browserInfo, Logger logger, ConfigurationProvider configurationProvider) {
        this.browserInfo = browserInfo;
        this.configurationProvider = configurationProvider;
        this.logger = logger;
    }

    /******** END - PUBLIC FOR TESTING PURPOSES ONLY ********/

    public RunningTest(IRenderingEyes eyes, EyesConnector eyesConnector, ConfigurationProvider configurationProvider, RenderBrowserInfo browserInfo, Logger logger) {
        this.eyes = eyes;
        this.eyesConnector = eyesConnector;
        this.browserInfo = browserInfo;
        this.configurationProvider = configurationProvider;
        this.logger = logger;
        this.appName = configurationProvider.get().getAppName();
        this.testName = configurationProvider.get().getTestName();
    }

    public Future<TestResultContainer> abort(boolean forceAbort, Throwable e) {
        logger.verbose("enter");
        logger.verbose(String.format("test info: %s", browserInfo));
        if (closeTask != null) {
            logger.verbose("close task already exists");
            if (forceAbort &&  closeTask.getType() == VisualGridTask.TaskType.CLOSE) {
                logger.verbose("force abort");
                removeAllCheckTasks();
                closeTask.setExceptionAndAbort(e);
            }
            return taskToFutureMapping.get(closeTask);
        }

        if (isOpenTaskIssued()) {
            openTask.setException(e);
        }

        logger.verbose("close task doesn't exists, aborting the test");
        removeAllCheckTasks();
        VisualGridTask abortTask = new VisualGridTask(new Configuration(configurationProvider.get()), null,
                eyesConnector, VisualGridTask.TaskType.ABORT, taskListener, null, this, null, null);
        visualGridTaskList.add(abortTask);
        this.closeTask = abortTask;
        FutureTask<TestResultContainer> futureTask = new FutureTask<>(abortTask);
        taskToFutureMapping.put(abortTask, futureTask);
        this.isCloseTaskIssued.set(true);
        return taskToFutureMapping.get(closeTask);
    }

    public IBatchCloser getBatchCloser() {
        return this.eyesConnector;
    }

    private void removeAllCheckTasks() {
        logger.verbose("enter");
        Iterator<VisualGridTask> iterator = visualGridTaskList.iterator();
        int counter = 0;
        while (iterator.hasNext()) {
            VisualGridTask next = iterator.next();
            if (next.getType() == VisualGridTask.TaskType.CHECK) {
                counter++;
                iterator.remove();
            }
        }
        logger.verbose("removed " + counter + " CHECK tasks from test");
    }

    public boolean isCloseTaskIssued() {
        return closeTask != null;
    }

    public boolean isTestOpen() {
        return isTestOpen.get();
    }

    boolean isCheckTaskReadyForRender(VisualGridTask checkTask) {
        if (!isTestOpen()) {
            return false;
        }

        synchronized (visualGridTaskList) {
            int notRenderedStepsCount = 0;
            for (VisualGridTask task : visualGridTaskList) {
                if (task.equals(checkTask)) {
                    break;
                }

                if (!task.isTaskReadyToCheck()) {
                    notRenderedStepsCount++;
                }
            }

            return notRenderedStepsCount < PARALLEL_STEPS_LIMIT;
        }
    }

    public List<VisualGridTask> getVisualGridTaskList() {
        return visualGridTaskList;
    }

    public ScoreTask getScoreTaskObjectByType(VisualGridTask.TaskType taskType) {
        if (!this.isTestOpen.get() && taskType == VisualGridTask.TaskType.CHECK) {
            return null;
        }

        int score = 0;
        synchronized (this.visualGridTaskList) {
            if (this.visualGridTaskList.isEmpty()) {
                return null;
            }

            for (VisualGridTask visualGridTask : this.visualGridTaskList) {
                if (visualGridTask.wasRenderTaskCreated() && visualGridTask.getType() == VisualGridTask.TaskType.CHECK) {
                    score++;
                }
            }

            VisualGridTask chosenVisualGridTask = this.visualGridTaskList.get(0);
            if (chosenVisualGridTask.getType() != taskType || chosenVisualGridTask.isSent()) {
                return null;
            }

            return new ScoreTask(chosenVisualGridTask, score);
        }
    }

    public synchronized FutureTask<TestResultContainer> getNextCloseTask() {
        logger.verbose("enter");
        if (visualGridTaskList.isEmpty() || !isCloseTaskIssued.get()) {
            logger.verbose("exit with null");
            return null;
        }

        VisualGridTask visualGridTask = visualGridTaskList.get(0);
        VisualGridTask.TaskType type = visualGridTask.getType();
        if (type != VisualGridTask.TaskType.CLOSE && type != VisualGridTask.TaskType.ABORT) {
            return null;
        }

        logger.verbose("locking visualGridTaskList");
        synchronized (visualGridTaskList) {
            logger.verbose("removing visualGridTask " + visualGridTask.toString() + " and exiting");
            visualGridTaskList.remove(visualGridTask);
            logger.verbose("tasks in visualGridTaskList: " + visualGridTaskList.size());
        }
        logger.verbose("releasing visualGridTaskList");
        return taskToFutureMapping.get(visualGridTask);
    }

    public RenderBrowserInfo getBrowserInfo() {
        return browserInfo;
    }

    public VisualGridTask open() {
        logger.verbose(String.format("test info: %s", browserInfo));
        logger.verbose("adding Open visualGridTask...");
        VisualGridTask visualGridTask = new VisualGridTask(new Configuration(configurationProvider.get()), null,
                eyesConnector, VisualGridTask.TaskType.OPEN, taskListener, null, this, null, null);
        openTask = visualGridTask;
        FutureTask<TestResultContainer> futureTask = new FutureTask<>(visualGridTask);
        this.taskToFutureMapping.put(visualGridTask, futureTask);
        logger.verbose("locking visualGridTaskList");
        synchronized (this.visualGridTaskList) {
            this.visualGridTaskList.add(visualGridTask);
            logger.verbose("Open visualGridTask was added: " + visualGridTask.toString());
            logVGTasksList(visualGridTaskList);
        }
        logger.verbose("releasing visualGridTaskList");
        return visualGridTask;
    }

    public FutureTask<TestResultContainer> close() {
        logger.verbose(String.format("test info: %s", browserInfo));
        VisualGridTask lastVisualGridTask;
        if (!this.visualGridTaskList.isEmpty()) {
            logger.verbose("visual grid tasks list not empty");
            lastVisualGridTask = this.visualGridTaskList.get(visualGridTaskList.size() - 1);
            VisualGridTask.TaskType type = lastVisualGridTask.getType();
            if (type == VisualGridTask.TaskType.CLOSE || type == VisualGridTask.TaskType.ABORT) {
                closeTask = lastVisualGridTask;
                logger.verbose("returning last task future (type of task: " + type + ")");
                return taskToFutureMapping.get(lastVisualGridTask);
            }
        } else {
            if (closeTask != null) {
                logger.verbose("returning future of close task");
                return taskToFutureMapping.get(closeTask);
            }

            logger.verbose("task list is empty and close task doesn't exist");
            if (!isTestOpen()) {
                logger.verbose("test never opened, returning null");
                return null;
            }
        }

        logger.verbose("adding close visualGridTask...");
        VisualGridTask visualGridTask = new VisualGridTask(new Configuration(configurationProvider.get()), null,
                eyesConnector, VisualGridTask.TaskType.CLOSE, taskListener, null, this, null, null);
        FutureTask<TestResultContainer> futureTask = new FutureTask<>(visualGridTask);
        closeTask = visualGridTask;
        isCloseTaskIssued.set(true);
        this.taskToFutureMapping.put(visualGridTask, futureTask);
        logger.verbose("locking visualGridTaskList");
        synchronized (visualGridTaskList) {
            this.visualGridTaskList.add(visualGridTask);
            logger.verbose("Close visualGridTask was added: " + visualGridTask.toString());
            logVGTasksList(this.visualGridTaskList);
        }
        logger.verbose("releasing visualGridTaskList");
        return this.taskToFutureMapping.get(visualGridTask);
    }

    private void logVGTasksList(List<VisualGridTask> visualGridTaskList) {
        logger.verbose("tasks in visualGridTaskList: " + visualGridTaskList.size());
        if (visualGridTaskList.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (VisualGridTask vgt : visualGridTaskList) {
                sb.append(vgt.getType()).append(" ; ");
            }
            logger.verbose(sb.toString());
        }
    }

    public VisualGridTask check(ICheckSettings checkSettings, List<VisualGridSelector[]> regionSelectors, String source) {
        logger.verbose(String.format("test info: %s", browserInfo));
        logger.verbose("adding check visualGridTask...");
        VisualGridTask visualGridTask = new VisualGridTask(new Configuration(configurationProvider.get()), null,
                eyesConnector, VisualGridTask.TaskType.CHECK, taskListener, checkSettings, this, regionSelectors, source);
        logger.verbose("locking visualGridTaskList");
        synchronized (visualGridTaskList) {
            this.visualGridTaskList.add(visualGridTask);
            logger.verbose("Check VisualGridTask was added: " + visualGridTask.toString());
            logVGTasksList(visualGridTaskList);
        }
        logger.verbose("releasing visualGridTaskList");
        this.taskToFutureMapping.get(visualGridTask);
        return visualGridTask;
    }

    /**
     * @return true if the only task left is CLOSE task
     */
    public boolean isTestReadyToClose() {
        if (visualGridTaskList.size() != 1) {
            return false;
        }

        for (VisualGridTask visualGridTask : visualGridTaskList) {
            if (visualGridTask.getType() == VisualGridTask.TaskType.CLOSE || visualGridTask.getType() == VisualGridTask.TaskType.ABORT)
                return true;
        }

        return false;
    }

    public boolean isTestClose() {
        return isTestClose.get();
    }

    public void setTestInExceptionMode(Throwable e) {
        this.isTestInExceptionMode.set(true);

        if (closeTask != null) {
            logger.verbose("locking visualGridTaskList.");
            synchronized (visualGridTaskList) {
                removeAllCheckTasks();
                if (closeTask != null) {
                    if (!visualGridTaskList.contains(closeTask)) {
                        this.visualGridTaskList.add(closeTask);
                    }
                    closeTask.setExceptionAndAbort(e);
                }
            }
        }
        if (openTask != null) {
            openTask.setExceptionAndAbort(e);
        }
        logger.verbose("releasing visualGridTaskList.");
    }

    Logger getLogger() {
        return logger;
    }

    public boolean isOpenTaskIssued() {
        return openTask != null;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setListener(IRenderingEyes.EyesListener eyesListener) {
        this.listener = eyesListener;
    }

    public boolean isServerConcurrencyLimitReached() {
        return eyesConnector.isServerConcurrencyLimitReached();
    }
}