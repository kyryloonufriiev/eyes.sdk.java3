package com.applitools.eyes.selenium.rendering;

import com.applitools.ICheckSettings;
import com.applitools.ICheckSettingsInternal;
import com.applitools.eyes.*;
import com.applitools.eyes.config.ISeleniumConfigurationGetter;
import com.applitools.eyes.config.ISeleniumConfigurationProvider;
import com.applitools.eyes.config.ISeleniumConfigurationSetter;
import com.applitools.eyes.fluent.CheckSettings;
import com.applitools.eyes.fluent.GetFloatingRegion;
import com.applitools.eyes.fluent.GetRegion;
import com.applitools.eyes.selenium.fluent.ISeleniumCheckTarget;
import com.applitools.eyes.selenium.fluent.SeleniumCheckSettings;
import com.applitools.eyes.visualgridclient.model.*;
import com.applitools.eyes.visualgridclient.services.*;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisualGridEyes implements IRenderingEyes {

    private Logger logger;

    private String apiKey;
    private String serverUrl;

    private final VisualGridRunner renderingGridManager;
    private List<RunningTest> testList = Collections.synchronizedList(new ArrayList<RunningTest>());
    private final List<RunningTest> testsInCloseProcess = Collections.synchronizedList(new ArrayList<RunningTest>());
    private AtomicBoolean isVGEyesClosed = new AtomicBoolean(false);
    private AtomicBoolean isVGEyesIssuedOpenTasks = new AtomicBoolean(false);
    private IRenderingEyes.EyesListener listener;
    private AbstractProxySettings proxy;

    private String PROCESS_RESOURCES;
    private JavascriptExecutor jsExecutor;
    private WebDriver webDriver;
    private RenderingInfo renderingInfo;
    private IEyesConnector VGEyesConnector;
    private IDebugResourceWriter debugResourceWriter;
    private String url;
    private List<Future<TestResultContainer>> futures = null;
    private String branchName = null;
    private String parentBranchName = null;
    private Boolean isDisabled;
    private IServerConnector serverConnector = null;
    private ISeleniumConfigurationProvider configProvider;

    private static final String GET_ELEMENT_XPATH_JS =
            "var el = arguments[0];" +
                    "var xpath = '';" +
                    "do {" +
                    " var parent = el.parentElement;" +
                    " var index = 1;" +
                    " if (parent !== null) {" +
                    "  var children = parent.children;" +
                    "  for (var childIdx in children) {" +
                    "    var child = children[childIdx];" +
                    "    if (child === el) break;" +
                    "    if (child.tagName === el.tagName) index++;" +
                    "  }" +
                    "}" +
                    "xpath = '/' + el.tagName + '[' + index + ']' + xpath;" +
                    " el = parent;" +
                    "} while (el !== null);" +
                    "return '/' + xpath;";


    {
        try {
            PROCESS_RESOURCES = GeneralUtils.readToEnd(VisualGridEyes.class.getResourceAsStream("/processPageAndSerialize.js"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public VisualGridEyes(VisualGridRunner renderingGridManager, ISeleniumConfigurationProvider configProvider) {
        this.configProvider = configProvider;
        ArgumentGuard.notNull(renderingGridManager, "renderingGridManager");
        this.renderingGridManager = renderingGridManager;
        this.logger = renderingGridManager.getLogger();
    }

    private RunningTest.RunningTestListener testListener = new RunningTest.RunningTestListener() {
        @Override
        public void onTaskComplete(Task task, RunningTest test) {
            switch (task.getType()) {
                case CLOSE:
                case ABORT:
                    boolean isVGEyesClosed = true;
                    for (RunningTest runningTest : testList) {
                        isVGEyesClosed &= runningTest.isTestClose();
                    }
                    VisualGridEyes.this.isVGEyesClosed.set(isVGEyesClosed);
                    break;
                case OPEN:

            }

            if (VisualGridEyes.this.listener != null) {
                VisualGridEyes.this.listener.onTaskComplete(task, VisualGridEyes.this);
            }
        }

        @Override
        public void onRenderComplete() {
            logger.verbose("enter");
            VisualGridEyes.this.listener.onRenderComplete();
            logger.verbose("exit");
        }
    };


    /**
     * Sets a handler of log messages generated by this API.
     *
     * @param logHandler Handles log messages generated by this API.
     */
    public void setLogHandler(LogHandler logHandler) {
        if (getIsDisabled()) return;
        LogHandler currentLogHandler = logger.getLogHandler();
        this.logger = new Logger();
        this.logger.setLogHandler(new MultiLogHandler(currentLogHandler, logHandler));

        if (currentLogHandler.isOpen() && !logHandler.isOpen()) {
            logHandler.open();
        }
    }

    public WebDriver open(WebDriver webDriver) {
        if (getIsDisabled()) return webDriver;
        logger.verbose("enter");

        ArgumentGuard.notNull(webDriver, "webDriver");
        initDriver(webDriver);

        logger.verbose("getting all browsers info...");
        List<RenderBrowserInfo> browserInfoList = getConfigGetter().getBrowsersInfo();
        logger.verbose("creating test descriptors for each browser info...");
        for (RenderBrowserInfo browserInfo : browserInfoList) {
            logger.verbose("creating test descriptor");
            RunningTest test = new RunningTest(createVGEyesConnector(browserInfo), configProvider, browserInfo, logger, testListener);
            this.testList.add(test);
        }

        logger.verbose(String.format("opening %d tests...", testList.size()));
        this.renderingGridManager.open(this, renderingInfo);
        logger.verbose("done");
        return webDriver;
    }

    private IEyesConnector createVGEyesConnector(RenderBrowserInfo browserInfo) {
        logger.verbose("creating VisualGridEyes server connector");
        EyesConnector VGEyesConnector = new EyesConnector(browserInfo, renderingGridManager.getRateLimiter());
        if (browserInfo.getEmulationInfo() != null) {
            VGEyesConnector.setDevice(browserInfo.getEmulationInfo().getDeviceName());
        }
        VGEyesConnector.setLogHandler(this.logger.getLogHandler());
        VGEyesConnector.setProxy(this.proxy);
        VGEyesConnector.setBatch(getConfigGetter().getBatch());
        VGEyesConnector.setBranchName(this.branchName);
        VGEyesConnector.setParentBranchName(parentBranchName);
        if (serverConnector != null) {
            VGEyesConnector.setServerConnector(serverConnector);
        }

        URI serverUri = this.getServerUrl();
        if (serverUri != null) {
            VGEyesConnector.setServerUrl(serverUri.toString());
        }

        String apiKey = this.getApiKey();
        if (apiKey != null) {
            VGEyesConnector.setApiKey(apiKey);
        } else {
            throw new EyesException("Missing API key");
        }

        if (this.renderingInfo == null) {
            logger.verbose("initializing rendering info...");
            this.renderingInfo = VGEyesConnector.getRenderingInfo();
        }
        VGEyesConnector.setRenderInfo(this.renderingInfo);

        this.VGEyesConnector = VGEyesConnector;
        return VGEyesConnector;
    }

    private void initDriver(WebDriver webDriver) {
        this.webDriver = webDriver;
        if (webDriver instanceof JavascriptExecutor) {
            this.jsExecutor = (JavascriptExecutor) webDriver;
        }
        String currentUrl = webDriver.getCurrentUrl();
        this.url = currentUrl;
    }

    public RunningTest getNextTestToClose() {
        synchronized (testsInCloseProcess) {
            for (RunningTest runningTest : testList) {
                if (!runningTest.isTestClose() && runningTest.isTestReadyToClose() && !this.testsInCloseProcess.contains(runningTest)) {
                    this.testsInCloseProcess.add(runningTest);
                    return runningTest;
                }
            }
        }
        return null;
    }

    public List<Future<TestResultContainer>> close() {
        if (getIsDisabled()) return null;
        futures = closeAndReturnResults();
        return futures;
    }

    public List<Future<TestResultContainer>> close(boolean throwException) {
        if (getIsDisabled()) return null;
        futures = closeAndReturnResults();
        return futures;
    }

    public void abortIfNotClosed() {
    }

    public boolean getIsOpen() {
        return !isEyesClosed();
    }

    public String getApiKey() {
        return this.apiKey == null ? this.renderingGridManager.getApiKey() : this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setIsDisabled(Boolean disabled) {
        this.isDisabled = disabled;
    }

    public boolean getIsDisabled() {
        return this.isDisabled == null ? this.renderingGridManager.getIsDisabled() : this.isDisabled;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public void setParentBranchName(String branchName) {
        this.parentBranchName = branchName;
    }

    public URI getServerUrl() {
        if (this.VGEyesConnector != null) {
            URI uri = this.VGEyesConnector.getServerUrl();
            if (uri != null) return uri;
        }
        String str = this.serverUrl == null ? this.renderingGridManager.getServerUrl() : this.serverUrl;
        return str == null ? null : URI.create(str);
    }

    private List<Future<TestResultContainer>> closeAndReturnResults() {
        if (getIsDisabled()) return new ArrayList<>();
        if (this.futures != null) {
            return futures;
        }
        List<Future<TestResultContainer>> futureList;
        logger.verbose("enter " + getConfigGetter().getBatch());
        futureList = new ArrayList<>();
        try {
            for (RunningTest runningTest : testList) {
                logger.verbose("running test name: " + getConfigGetter().getTestName());
                logger.verbose("is current running test open: " + runningTest.isTestOpen());
                logger.verbose("is current running test ready to close: " + runningTest.isTestReadyToClose());
                logger.verbose("is current running test closed: " + runningTest.isTestClose());
                if (!runningTest.isTestClose()) {
                    logger.verbose("closing current running test");
                    FutureTask<TestResultContainer> closeFuture = runningTest.close();
                    logger.verbose("adding closeFuture to futureList");
                    futureList.add(closeFuture);
                }
            }
            futures = futureList;
            this.renderingGridManager.close(this);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return futureList;
    }

    @Override
    public synchronized ScoreTask getBestScoreTaskForCheck() {

        int bestScore = -1;

        ScoreTask currentBest = null;
        for (RunningTest runningTest : testList) {

            List<Task> taskList = runningTest.getTaskList();

            Task task;
            synchronized (taskList) {
                if (taskList.isEmpty()) continue;

                task = taskList.get(0);
                if (!runningTest.isTestOpen() || task.getType() != Task.TaskType.CHECK || !task.isTaskReadyToCheck())
                    continue;
            }


            ScoreTask scoreTask = runningTest.getScoreTaskObjectByType(Task.TaskType.CHECK);

            if (scoreTask == null) continue;

            if (bestScore < scoreTask.getScore()) {
                currentBest = scoreTask;
                bestScore = scoreTask.getScore();
            }
        }
        return currentBest;
    }

    @Override
    public ScoreTask getBestScoreTaskForOpen() {
        int bestMark = -1;
        ScoreTask currentBest = null;
        synchronized (testList) {
            for (RunningTest runningTest : testList) {

                ScoreTask currentScoreTask = runningTest.getScoreTaskObjectByType(Task.TaskType.OPEN);
                if (currentScoreTask == null) continue;

                if (bestMark < currentScoreTask.getScore()) {
                    bestMark = currentScoreTask.getScore();
                    currentBest = currentScoreTask;

                }
            }
        }
        return currentBest;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public boolean isEyesClosed() {
        boolean isVGEyesClosed = true;
        for (RunningTest runningTest : testList) {
            isVGEyesClosed = isVGEyesClosed && runningTest.isTestClose();
        }
        return isVGEyesClosed;
    }

    public void setListener(EyesListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the proxy settings to be used by the rest client.
     *
     * @param abstractProxySettings The proxy settings to be used by the rest client.
     *                              If {@code null} then no proxy is set.
     */
    public void setProxy(AbstractProxySettings abstractProxySettings) {
        this.proxy = abstractProxySettings;
    }

    public void check(String name, ICheckSettings checkSettings) {
        if (getIsDisabled()) return;
        ArgumentGuard.notNull(checkSettings, "checkSettings");
        trySetTargetSelector((SeleniumCheckSettings)checkSettings);
        checkSettings = checkSettings.withName(name);
        this.check(checkSettings);
    }

    public void check(ICheckSettings checkSettings) {
        if (getIsDisabled()) return;
        logger.verbose("enter");

        ArgumentGuard.notOfType(checkSettings, ICheckSettings.class, "checkSettings");

        List<Task> openTasks = addOpenTaskToAllRunningTest();

        List<Task> taskList = new ArrayList<>();

        ICheckSettingsInternal settingsInternal = (ICheckSettingsInternal) checkSettings;

        String sizeMode = settingsInternal.getSizeMode();

        //First check config
        if (sizeMode == null) {
            if (this.getConfigGetter().getForceFullPageScreenshot()) {
                settingsInternal.setSizeMode("full-page");
            }
        }

        String domCaptureScript = "var callback = arguments[arguments.length - 1]; return (" + PROCESS_RESOURCES + ")().then(JSON.stringify).then(callback, function(err) {callback(err.stack || err.toString())})";

        logger.verbose("Dom extraction starting   (" + checkSettings.toString() + ")");
        String scriptResult = (String) this.jsExecutor.executeAsyncScript(domCaptureScript);

        logger.verbose("Dom extracted  (" + checkSettings.toString() + ")");

        List<VisualGridSelector[]> regionsXPaths = getRegionsXPaths(checkSettings);

        logger.verbose("regionXPaths : "+regionsXPaths);

        for (final RunningTest test : testList) {
            Task checkTask = test.check(checkSettings, regionsXPaths);
            taskList.add(checkTask);
        }

        logger.verbose("added check tasks  (" + checkSettings.toString() + ")");

        this.renderingGridManager.check(checkSettings, debugResourceWriter, scriptResult,
                this.VGEyesConnector, taskList, openTasks, checkSettings,
                new VisualGridRunner.RenderListener() {
                    @Override
                    public void onRenderSuccess() {

                    }

                    @Override
                    public void onRenderFailed(Exception e) {
                        GeneralUtils.logExceptionStackTrace(logger, e);
                    }
                }, regionsXPaths);

        logger.verbose("created renderTask  (" + checkSettings.toString() + ")");
    }

    private void trySetTargetSelector(SeleniumCheckSettings checkSettings)
    {
        ISeleniumCheckTarget seleniumCheckTarget = checkSettings;
        WebElement element = seleniumCheckTarget.getTargetElement();
        if (element == null)
        {
            By targetSelector = seleniumCheckTarget.getTargetSelector();
            if (targetSelector != null)
            {
                element = webDriver.findElement(targetSelector);
            }
        }

        if (element == null) return;

        String xpath = (String)jsExecutor.executeScript(GET_ELEMENT_XPATH_JS, element);
        VisualGridSelector vgs = new VisualGridSelector(xpath, "target");
        checkSettings.setTargetSelector(vgs);
    }
    private synchronized List<Task> addOpenTaskToAllRunningTest() {
        logger.verbose("enter");
        List<Task> tasks = new ArrayList<>();
        if (!this.isVGEyesIssuedOpenTasks.get()) {
            for (RunningTest runningTest : testList) {
                Task task = runningTest.open();
                tasks.add(task);
            }
            logger.verbose("calling addOpenTaskToAllRunningTest.open");
            this.isVGEyesIssuedOpenTasks.set(true);
        }
        logger.verbose("exit");
        return tasks;
    }

    public Logger getLogger() {
        return logger;
    }

    public List<RunningTest> getAllRunningTests() {
        return testList;
    }

    public void setDebugResourceWriter(IDebugResourceWriter debugResourceWriter) {
        this.debugResourceWriter = debugResourceWriter;
    }

    @Override
    public String toString() {
        return "SelenuimVGEyes - url: " + url;
    }

    public void setServerConnector(IServerConnector serverConnector) {
        this.serverConnector = serverConnector;
    }


    public AbstractProxySettings getProxy() {
        return this.proxy;
    }

    /**
     * @return The full agent id composed of both the base agent id and the
     * user given agent id.
     */
    public String getFullAgentId() {
        String agentId = getConfigGetter().getAgentId();
        if (agentId == null) {
            return getBaseAgentId();
        }
        return String.format("%s [%s]", agentId, getBaseAgentId());
    }

    private ISeleniumConfigurationGetter getConfigGetter() {
        return this.configProvider.get();
    }

    private ISeleniumConfigurationSetter getConfigSetter() {
        return this.configProvider.set();
    }

    public String getBaseAgentId() {
        return "eyes.selenium.java/3.149.1-beta";
    }

    /**
     * Sets the batch in which context future tests will run or {@code null}
     * if tests are to run standalone.
     *
     * @param batch The batch info to set.
     */
    public void setBatch(BatchInfo batch) {
        if (isDisabled) {
            logger.verbose("Ignored");
            return;
        }

        logger.verbose("setBatch(" + batch + ")");

        this.getConfigSetter().setBatch(batch);
    }

    private List<VisualGridSelector[]> getRegionsXPaths(ICheckSettings checkSettings) {
        List<VisualGridSelector[]> result = new ArrayList<>();
        ICheckSettingsInternal csInternal = (ICheckSettingsInternal) checkSettings;
        List<WebElementRegion>[] elementLists = collectSeleniumRegions(csInternal);
        for (List<WebElementRegion> elementList : elementLists) {
            List<VisualGridSelector> xpaths = new ArrayList<>();
            for (WebElementRegion webElementRegion : elementList) {
                if (webElementRegion.getElement() == null) continue;
                String xpath = (String) jsExecutor.executeScript(GET_ELEMENT_XPATH_JS, webElementRegion.getElement());
                xpaths.add(new VisualGridSelector(xpath, webElementRegion.getRegion()));
            }
            result.add(xpaths.toArray(new VisualGridSelector[0]));
        }

        return result;
    }

    private List<WebElementRegion>[] collectSeleniumRegions(ICheckSettingsInternal csInternal) {
        CheckSettings settings = (CheckSettings) csInternal;
        GetRegion[] ignoreRegions = settings.getIgnoreRegions();
        GetRegion[] layoutRegions = settings.getLayoutRegions();
        GetRegion[] strictRegions = settings.getStrictRegions();
        GetRegion[] contentRegions = settings.getContentRegions();
        GetFloatingRegion[] floatingRegions = settings.getFloatingRegions();

        List<WebElementRegion> ignoreElements = getElementsFromRegions(Arrays.asList(ignoreRegions));
        List<WebElementRegion> layoutElements = getElementsFromRegions(Arrays.asList(layoutRegions));
        List<WebElementRegion> strictElements = getElementsFromRegions(Arrays.asList(strictRegions));
        List<WebElementRegion> contentElements = getElementsFromRegions(Arrays.asList(contentRegions));
        List<WebElementRegion> floatingElements = getElementsFromRegions(Arrays.asList(floatingRegions));


        WebElement targetElement = ((ISeleniumCheckTarget)csInternal).getTargetElement();
        if (targetElement == null)
        {
            By targetSelector = ((ISeleniumCheckTarget)csInternal).getTargetSelector();
            if (targetSelector != null)
            {
                targetElement = webDriver.findElement(targetSelector);
            }
        }

        WebElementRegion target = new WebElementRegion(targetElement, "target");
        List<WebElementRegion> targetElementList = new ArrayList<>();
        targetElementList.add(target);

        List<WebElementRegion>[] lists = new List[]{ignoreElements, layoutElements, strictElements, contentElements, floatingElements, targetElementList};
        return lists;
    }

    private List<WebElementRegion> getElementsFromRegions(List regionsProvider) {
        List<WebElementRegion> elements = new ArrayList<>();
        for (Object getRegion : regionsProvider) {
            if (getRegion instanceof IGetSeleniumRegion)
            {
                IGetSeleniumRegion getSeleniumRegion = (IGetSeleniumRegion) getRegion;
                List<WebElement> webElements = getSeleniumRegion.getElements(webDriver);
                for (WebElement webElement : webElements) {
                    elements.add(new WebElementRegion(webElement, getRegion));
                }
            }
        }
        return elements;
    }

}
