package com.applitools.eyes.selenium.rendering;

import com.applitools.ICheckSettings;
import com.applitools.eyes.*;
import com.applitools.eyes.capture.AppOutputWithScreenshot;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.fluent.ICheckSettingsInternal;
import com.applitools.eyes.visualgrid.services.IEyesConnector;
import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.eyes.visualgrid.model.*;
import com.applitools.eyes.visualgrid.services.VisualGridTask;

import java.net.URI;
import java.net.URL;
import java.util.List;

class EyesConnector extends EyesBase implements IEyesConnector {

    private RenderBrowserInfo browserInfo;
    private String userAgent;
    private String device;
    private RectangleSize deviceSize;
    private String appName;
    private String testName;
    private com.applitools.eyes.config.Configuration configuration;

    public EyesConnector(com.applitools.eyes.config.Configuration configuration, List<PropertyData> properties, RenderBrowserInfo browserInfo) {
        this.configuration = configuration;
        this.browserInfo = browserInfo;
        if (properties != null) {
            for (PropertyData property : properties) {
                this.addProperty(property);
            }
        }
        //this.setServerConnector(new ThrottlingServerConnector(this.serverConnector, rateLimiter));
    }

    /**
     * ﻿Starts a new test without setting the viewport size of the AUT.
     * @param config
     */
    public void open(Configuration config) {
        this.configuration = config;
        this.appName = config.getAppName();
        this.testName = config.getTestName();
        logger.verbose("opening EyesConnector with viewport size: " + browserInfo.getViewportSize());
        openBase();
    }

    @Override
    public TestResults close(boolean throwExceptionOn, Configuration configuration) {
        this.configuration = configuration;
        return this.close(throwExceptionOn);
    }

    @Override
    public IResourceFuture getResource(URL url, String userAgent) {
        return this.serverConnector.downloadResource(url, userAgent);
    }

    @Override
    public IPutFuture renderPutResource(RunningRender runningRender, RGridResource resource, String UserAgent) {
        return this.serverConnector.renderPutResource(runningRender, resource, userAgent, null);
    }


    @Override
    public List<RunningRender> render(RenderRequest... renderRequests) {
        return this.serverConnector.render(renderRequests);
    }

    @Override
    public List<RenderStatusResults> renderStatusById(String... renderIds) {
        return this.serverConnector.renderStatusById(renderIds);
    }

    @Override
    public IResourceFuture createResourceFuture(RGridResource rg) {
        return this.serverConnector.createResourceFuture(rg, userAgent);
    }

    @Override
    public MatchResult matchWindow(Configuration configuration, String resultImageURL, String domLocation, ICheckSettings checkSettings,
                                   List<? extends IRegion> regions, List<VisualGridSelector[]> regionSelectors, Location location,
                                   String renderId, String source) {

        this.configuration = configuration;
        ICheckSettingsInternal checkSettingsInternal = (ICheckSettingsInternal) checkSettings;
        if(checkSettingsInternal.getStitchContent() == null){
            checkSettings.fully();
        }

        MatchWindowTask matchWindowTask = new MatchWindowTask(this.logger, this.serverConnector, this.runningSession, configuration.getMatchTimeout(), this);

        ImageMatchSettings imageMatchSettings = matchWindowTask.createImageMatchSettings(checkSettingsInternal, this);

        String tag = checkSettingsInternal.getName();

        AppOutput appOutput = new AppOutput(tag, null, domLocation, resultImageURL);
        AppOutputWithScreenshot appOutputWithScreenshot = new AppOutputWithScreenshot(appOutput, null, location);

        return matchWindowTask.performMatch(appOutputWithScreenshot, tag, false, checkSettingsInternal, imageMatchSettings, regions, regionSelectors, this, renderId, source);
    }

    /**
     * Starts a test.
     * @param appName    The name of the application under test.
     * @param testName   The test name.
     * @param dimensions Determines the resolution used for the baseline.
     *                   {@code null} will automatically grab the resolution from the image.
     */
    public void open(String appName, String testName,
                     RectangleSize dimensions) {
        openBase(appName, testName, dimensions, null);
    }

    @Override
    protected String getBaseAgentId() {
        return "eyes.selenium.visualgrid.java/3.157.4";
    }

    @Override
    protected RectangleSize getViewportSize() {
        return null;
    }

    @Override
    protected Configuration setViewportSize(RectangleSize size) {
        logger.log("WARNING setViewportSize() was called in Visual-Grid context");
        return (Configuration) this.configuration;
    }

    @Override
    protected String getInferredEnvironment() {
        return "useragent:" + userAgent;
    }

    @Override
    protected EyesScreenshot getScreenshot(ICheckSettingsInternal checkSettingsInternal) {
        return null;
    }

    @Override
    protected String getTitle() {
        return null;
    }

    @Override
    protected String getAUTSessionId() {
        return null;
    }

    public void setRenderInfo(RenderingInfo renderInfo) {
        this.renderInfo = renderInfo;
        this.serverConnector.setRenderingInfo(renderInfo);
    }

    @Override
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    protected void openLogger() {
        // do nothing.
    }

    @Override
    protected void closeLogger() {
        // do nothing.
    }

    @Override
    public String tryCaptureDom() {
        return null;
    }

    @Override
    public void setDevice(String device) {
        this.device = device;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This override also checks for mobile operating system.
     */
    @Override
    protected AppEnvironment getAppEnvironment() {
        AppEnvironment appEnv = super.getAppEnvironment();
        appEnv.setDeviceInfo(device);
        if (userAgent == null) {
            appEnv.setOs(VisualGridTask.toPascalCase(browserInfo.getPlatform()));
            String browserName = browserInfo.getBrowserType();
            if (browserName.equals("ie")) {
                browserName = "IE 11";
            } else if (browserName.equals("ie10")) {
                browserName = "IE 10";
            } else {
                browserName = VisualGridTask.toPascalCase(browserName);
            }
            appEnv.setHostingApp(browserName);
        }
        logger.log("Done!");
        return appEnv;
    }

    @Override
    public RectangleSize getDeviceSize() {
        return deviceSize;
    }

    @Override
    public void setDeviceSize(RectangleSize deviceSize) {
        this.deviceSize = deviceSize;
    }

    @Override
    public RunningSession getSession() {
        return this.runningSession;
    }

    @Override
    protected RectangleSize getViewportSizeForOpen() {
        if (device != null) {
            return deviceSize;
        } else if (browserInfo.getViewportSize() != null) {
            return browserInfo.getViewportSize();
        } else {
            //this means it's a emulationInfo
            if (browserInfo.getEmulationInfo() instanceof EmulationDevice) {
                EmulationDevice emulationDevice = (EmulationDevice) browserInfo.getEmulationInfo();
                return new RectangleSize(emulationDevice.getWidth(), emulationDevice.getHeight());
            }
        }
        return super.getViewportSizeForOpen();
    }

    @Override
    protected String getBaselineEnvName() {
        String baselineEnvName = this.browserInfo.getBaselineEnvName();
        if (baselineEnvName != null) {
            return baselineEnvName;
        }
        return configuration.getBaselineEnvName();
    }

    @Override
    protected String getAppName() {
        return this.appName;
    }

    @Override
    protected com.applitools.eyes.config.Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    protected String getTestName() {
        return this.testName;
    }


}
