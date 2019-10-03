package com.applitools.eyes.visualgrid.services;

import com.applitools.ICheckSettings;
import com.applitools.eyes.*;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.visualgrid.model.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public interface IEyesConnector {
    void log(String massage);

    Configuration setProxy(AbstractProxySettings proxy);

    void setLogHandler(LogHandler logHandler);

    Configuration setServerUrl(String serverUrl) throws URISyntaxException;

    URI getServerUrl();

    void open(Configuration configProvider);

    TestResults close(boolean throwExceptionOn, Configuration configuration);

    TestResults abortIfNotClosed();

    IResourceFuture getResource(URL url, String userAgent);

    RenderingInfo getRenderingInfo();

    IPutFuture renderPutResource(RunningRender runningRender, RGridResource resource, String userAgent);

    List<RunningRender> render(RenderRequest... renderRequests);

    List<RenderStatusResults> renderStatusById(String... renderIds);

    IResourceFuture createResourceFuture(RGridResource rg);

    MatchResult matchWindow(Configuration configuration, String resultImageURL, String domLocation, ICheckSettings checkSettings,
                            List<? extends IRegion> regions, List<VisualGridSelector[]> regionSelectors, Location location,
                            String renderId, String source);

    void setRenderInfo(RenderingInfo renderingInfo);

    Configuration setBatch(BatchInfo batchInfo);

    void setUserAgent(String userAgent);

    String getApiKey();

    Configuration setApiKey(String apiKey);

    Configuration setBranchName(String branchName);

    Configuration setParentBranchName(String parentBranchName);

    void setDevice(String device);

    RectangleSize getDeviceSize();

    void setDeviceSize(RectangleSize deviceSize);

    RunningSession getSession();

    void addProperty(String name, String value);

    void clearProperties();
}
