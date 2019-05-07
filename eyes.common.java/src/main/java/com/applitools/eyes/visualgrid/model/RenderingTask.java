package com.applitools.eyes.visualgrid.model;

import com.applitools.ICheckSettings;
import com.applitools.ICheckSettingsInternal;
import com.applitools.eyes.IPutFuture;
import com.applitools.eyes.Logger;
import com.applitools.eyes.visualgrid.services.IEyesConnector;
import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.eyes.visualgrid.services.VisualGridRunner;
import com.applitools.eyes.visualgrid.services.VisualGridTask;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderingTask implements Callable<RenderStatusResults>, CompletableTask {

    private static final int MAX_FETCH_FAILS = 62;
    public static final String CDT = "x-applitools-html/cdt";
    public static final String FULLPAGE = "full-page";
    public static final String VIEWPORT = "viewport";
    public static final int HOUR = 60 * 60 * 1000;

    private final List<RenderTaskListener> listeners = new ArrayList<>();
    private IEyesConnector eyesConnector;
    private ICheckSettings checkSettings;
    private List<VisualGridTask> visualGridTaskList;
    private List<VisualGridTask> openVisualGridTaskList;
    private RenderingInfo renderingInfo;
    private final Map<String, IResourceFuture> fetchedCacheMap;
    private final Map<String, IPutFuture> putResourceCache;
    private Logger logger;
    private AtomicBoolean isTaskComplete = new AtomicBoolean(false);
    private AtomicBoolean isForcePutNeeded;
    private final List<VisualGridSelector[]> regionSelectors;
    private IDebugResourceWriter debugResourceWriter;
    private FrameData result = null;
    private AtomicInteger framesLevel = new AtomicInteger();
    private RGridDom dom = null;
    private boolean forceFullPageScreenshot;
    private Timer timer = new Timer("VG_StopWatch", true);
    private AtomicBoolean isTimeElapsed = new AtomicBoolean(false);

    private boolean isTaskStarted = false;
    private boolean isTaskCompleted = false;
    private boolean isTaskInException = false;

    public interface RenderTaskListener {
        void onRenderSuccess();

        void onRenderFailed(Exception e);
    }

    public RenderingTask(IEyesConnector eyesConnector, FrameData scriptResult, ICheckSettings checkSettings,
                         List<VisualGridTask> visualGridTaskList, List<VisualGridTask> openVisualGridTasks, VisualGridRunner renderingGridManager,
                         IDebugResourceWriter debugResourceWriter, RenderTaskListener listener, List<VisualGridSelector[]> regionSelectors, boolean forceFullPageScreenshot) {

        this.eyesConnector = eyesConnector;
        this.result = scriptResult;
        this.checkSettings = checkSettings;
        this.visualGridTaskList = visualGridTaskList;
        this.openVisualGridTaskList = openVisualGridTasks;
        this.renderingInfo = renderingGridManager.getRenderingInfo();
        this.fetchedCacheMap = renderingGridManager.getCachedResources();
        this.putResourceCache = renderingGridManager.getPutResourceCache();
        this.logger = renderingGridManager.getLogger();
        this.debugResourceWriter = debugResourceWriter;
        this.regionSelectors = regionSelectors;
        this.forceFullPageScreenshot = forceFullPageScreenshot;
        this.listeners.add(listener);
        String renderingGridForcePut = System.getenv("APPLITOOLS_RENDERING_GRID_FORCE_PUT");
        this.isForcePutNeeded = new AtomicBoolean(renderingGridForcePut != null && renderingGridForcePut.equalsIgnoreCase("true"));
    }

    @Override
    public RenderStatusResults call() {

        try {
            this.isTaskStarted = true;

            addRenderingTaskToOpenTasks();

            logger.verbose("enter");

            boolean isSecondRequestAlreadyHappened = false;

            logger.verbose("step 1");

            //Build RenderRequests
            RenderRequest[] requests = prepareDataForRG(result);

            logger.verbose("step 2");
            boolean stillRunning = true;
            int fetchFails = 0;
            boolean isForcePutAlreadyDone = false;
            List<RunningRender> runningRenders = null;
            do {
                try {

                    runningRenders = this.eyesConnector.render(requests);

                } catch (Exception e) {

                    Thread.sleep(1500);
                    logger.verbose("/render throws exception... sleeping for 1.5s");
                    GeneralUtils.logExceptionStackTrace(logger, e);
                    if (e.getMessage().contains("Second request, yet still some resources were not PUT in renderId")) {
                        if (isSecondRequestAlreadyHappened) {
                            logger.verbose("Second request already happened");
                        }
                        isSecondRequestAlreadyHappened = true;
//                        this.isForcePutNeeded.set(true);
                    }
                    logger.verbose("ERROR " + e.getMessage());
                    fetchFails++;
                }
                logger.verbose("step 3.1");
                if (runningRenders == null) {
                    logger.verbose("ERROR - runningRenders is null.");
                    continue;
                }

                for (int i = 0; i < requests.length; i++) {
                    RenderRequest request = requests[i];
                    request.setRenderId(runningRenders.get(i).getRenderId());
                }
                logger.verbose("step 3.2");

                RunningRender runningRender = runningRenders.get(0);
                RenderStatus worstStatus = runningRender.getRenderStatus();

                worstStatus = calcWorstStatus(runningRenders, worstStatus);

                boolean isNeedMoreDom = runningRender.isNeedMoreDom();

                if (isForcePutNeeded.get() && !isForcePutAlreadyDone) {
                    forcePutAllResources(requests[0].getResources(), runningRender);
                    isForcePutAlreadyDone = true;
                }

                logger.verbose("step 3.3");
                stillRunning = worstStatus == RenderStatus.NEED_MORE_RESOURCE || isNeedMoreDom || fetchFails > MAX_FETCH_FAILS;
                if (stillRunning) {
                    sendMissingResources(runningRenders, requests[0].getDom(), requests[0].getResources(), isNeedMoreDom);
                }

                logger.verbose("step 3.4");

            } while (stillRunning);

            Map<RunningRender, RenderRequest> mapping = mapRequestToRunningRender(runningRenders, requests);

            logger.verbose("step 4");
            pollRenderingStatus(mapping);

            isTaskCompleted = true;
        } catch (Throwable e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
            for (VisualGridTask visualGridTask : this.visualGridTaskList) {
                visualGridTask.setException(e);
            }
        }
        logger.verbose("Finished rendering task - exit");

        return null;
    }

    private void addRenderingTaskToOpenTasks() {
        if (this.openVisualGridTaskList != null) {
            for (VisualGridTask visualGridTask : openVisualGridTaskList) {
                visualGridTask.setRenderingTask(this);
            }
        }
    }

    private void forcePutAllResources(Map<String, RGridResource> resources, RunningRender runningRender) {
        RGridResource resource;
        List<IPutFuture> allPuts = new ArrayList<>();
        for (String url : resources.keySet()) {
            try {
                logger.verbose("trying to get url from map - " + url);
                IResourceFuture resourceFuture = fetchedCacheMap.get(url);
                if (resourceFuture == null) {
                    logger.verbose("fetchedCacheMap.get(url) == null trying dom");
                    if (url.equals(this.dom.getUrl())) {
                        resource = this.dom.asResource();
                    } else {
                        logger.verbose("Resource not found Exiting...");
                        return;
                    }
                } else {
                    resource = resourceFuture.get();
                    IPutFuture future = this.eyesConnector.renderPutResource(runningRender, resource);
                    logger.verbose("locking putResourceCache");
                    synchronized (putResourceCache) {
                        String contentType = resource.getContentType();
                        if (contentType != null && !contentType.equalsIgnoreCase(CDT)) {
                            putResourceCache.put(dom.getUrl(), future);
                        }
                        allPuts.add(future);
                    }
                }
            } catch (Exception e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }
        for (IPutFuture put : allPuts) {
            try {
                put.get();
            } catch (InterruptedException | ExecutionException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }

    }

    private void notifySuccessAllListeners() {
        for (RenderTaskListener listener : listeners) {
            listener.onRenderSuccess();
        }
    }

    private Map<RunningRender, RenderRequest> mapRequestToRunningRender(List<RunningRender> runningRenders, RenderRequest[] requests) {
        Map<RunningRender, RenderRequest> mapping = new HashMap<>();
        for (int i = 0; i < requests.length; i++) {
            RenderRequest request = requests[i];
            RunningRender runningRender = runningRenders.get(i);
            mapping.put(runningRender, request);
        }
        return mapping;
    }

    private RenderStatus calcWorstStatus(List<RunningRender> runningRenders, RenderStatus worstStatus) {
        LOOP:
        for (RunningRender runningRender : runningRenders) {
            switch (runningRender.getRenderStatus()) {
                case NEED_MORE_RESOURCE:
                    if (worstStatus == RenderStatus.RENDERED || worstStatus == RenderStatus.RENDERING) {
                        worstStatus = RenderStatus.NEED_MORE_RESOURCE;
                    }
                    break;
                case ERROR:
                    worstStatus = RenderStatus.ERROR;
                    break LOOP;
            }
        }
        return worstStatus;
    }

    private List<String> getRenderIds(Collection<RunningRender> runningRenders) {
        List<String> ids = new ArrayList<>();
        for (RunningRender runningRender : runningRenders) {
            ids.add(runningRender.getRenderId());
        }
        return ids;
    }

    private void sendMissingResources(List<RunningRender> runningRenders, RGridDom dom, Map<String, RGridResource> resources, boolean isNeedMoreDom) {
        logger.verbose("enter");
        List<IPutFuture> allPuts = new ArrayList<>();
        if (isNeedMoreDom) {
            RunningRender runningRender = runningRenders.get(0);
            IPutFuture future = null;
            try {
                future = this.eyesConnector.renderPutResource(runningRender, dom.asResource());
            } catch (JsonProcessingException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
            logger.verbose("locking putResourceCache");
            allPuts.add(future);
            logger.verbose("releasing putResourceCache");
        }

        logger.verbose("creating PutFutures for " + runningRenders.size() + " runningRenders");

        for (RunningRender runningRender : runningRenders) {
            createPutFutures(allPuts, runningRender, resources);
        }

        logger.verbose("calling future.get on " + allPuts.size() + " PutFutures");
        for (IPutFuture future : allPuts) {
            logger.verbose("calling future.get on " + future.toString());
            try {
                future.get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }
        logger.verbose("exit");
    }

    private void createPutFutures(List<IPutFuture> allPuts, RunningRender runningRender, Map<String, RGridResource> resources) {
        List<String> needMoreResources = runningRender.getNeedMoreResources();
        RGridResource resource;
        for (String url : needMoreResources) {
            if (putResourceCache.containsKey(url)) {
                IPutFuture putFuture = putResourceCache.get(url);
                if (!allPuts.contains(putFuture)) {
                    allPuts.add(putFuture);
                }
                continue;
            }

            //                    logger.verbose("trying to get url from map - " + url);
            IResourceFuture resourceFuture = fetchedCacheMap.get(url);
            if (resourceFuture == null) {
                logger.verbose("fetchedCacheMap.get(url) == null - " + url);
                logger.verbose("Resource put requested but never downloaded(maybe a Frame)");
                resource = resources.get(url);
            } else {
                try {
                    resource = resourceFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                    continue;
                }
//
            }
            logger.verbose("resource(" + resource.getUrl() + ") hash : " + resource.getSha256());
            IPutFuture future = this.eyesConnector.renderPutResource(runningRender, resource);
            String contentType = resource.getContentType();
            if (!putResourceCache.containsKey(url) && (contentType != null && !contentType.equalsIgnoreCase(CDT))) {
                synchronized (putResourceCache) {
                    putResourceCache.put(url, future);
                }
            }
            allPuts.add(future);

        }

    }

    private RenderRequest[] prepareDataForRG(FrameData result) throws ExecutionException, InterruptedException, JsonProcessingException {

        final Map<String, RGridResource> allBlobs = Collections.synchronizedMap(new HashMap<String, RGridResource>());
        Set<URL> resourceUrls = new HashSet<>();

        parseScriptResult(result, allBlobs, resourceUrls);

        logger.verbose("fetching " + resourceUrls.size() + " resources...");

        //Fetch all resources
        fetchAllResources(allBlobs, resourceUrls);
        if (!resourceUrls.isEmpty()) {
            logger.verbose("ERROR resourceUrl is not empty!!!!!***************************");
        }

        logger.verbose("done fetching resources.");

        int written = addBlobsToCache(allBlobs);

        logger.verbose("written " + written + " blobs to cache.");

        //Create RenderingRequest

        //Parse allBlobs to mapping
        Map<String, RGridResource> resourceMapping = new HashMap<>();
        for (String url : allBlobs.keySet()) {
            try {
                logger.verbose("trying to fetch - " + url);
                IResourceFuture iResourceFuture = this.fetchedCacheMap.get(url);
                if (iResourceFuture != null) {
                    RGridResource value = iResourceFuture.get(10, TimeUnit.SECONDS);
                    if (value.getContent() != null) resourceMapping.put(url, value);
                }
            } catch (Exception e) {
                logger.verbose("Couldn't download url = " + url);
            }
        }

        buildAllRGDoms(resourceMapping, result);

        List<RenderRequest> allRequestsForRG = buildRenderRequests(result, resourceMapping);

        RenderRequest[] asArray = allRequestsForRG.toArray(new RenderRequest[0]);

        if (debugResourceWriter != null && !(debugResourceWriter instanceof NullDebugResourceWriter)) {
            for (RenderRequest renderRequest : asArray) {
                for (RGridResource value : renderRequest.getResources().values()) {
                    this.debugResourceWriter.write(value);
                }
            }
        }

        logger.verbose("exit - returning renderRequest array of length: " + asArray.length);
        return asArray;
    }

    private void buildAllRGDoms(Map<String, RGridResource> resourceMapping, FrameData result) {
        URL baseUrl = result.getUrl();
        logger.verbose("baseUrl: " + baseUrl);
        List<FrameData> allFrame = result.getFrames();
        Map<String, RGridResource> mapping = new HashMap<>();
        for (FrameData frameObj : allFrame) {
            List<BlobData> allFramesBlobs = frameObj.getBlobs();
            @SuppressWarnings("unchecked")
            List<URL> allResourceUrls = frameObj.getResourceUrls();
            URL frameUrl = null;
            try {
                frameUrl = new URL(baseUrl, frameObj.getUrl().toString());
            } catch (MalformedURLException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
            for (BlobData blob : allFramesBlobs) {
                String blobUrl = blob.getUrl().toString();
                RGridResource rGridResource = resourceMapping.get(blobUrl);
                mapping.put(blobUrl, rGridResource);

            }
            for (URL resourceUrl : allResourceUrls) {
                RGridResource rGridResource = resourceMapping.get(resourceUrl.toString());
                mapping.put(resourceUrl.toString(), rGridResource);
            }
            List<CdtData> cdt = frameObj.getCdt();
            RGridDom rGridDom = new RGridDom(cdt, mapping, frameUrl.toString(), logger, "buildAllRGDoms");
            try {
                resourceMapping.put(frameUrl.toString(), rGridDom.asResource());
                buildAllRGDoms(resourceMapping, frameObj);
            } catch (JsonProcessingException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void parseScriptResult(FrameData result, Map<String, RGridResource> allBlobs, Set<URL> resourceUrls) {

        Base64 codec = new Base64();

        URL baseUrl = result.getUrl();

        logger.verbose("baseUrl: " + baseUrl);

        parseBlobs(allBlobs, codec, baseUrl, result.getBlobs());

        parseResourceUrls(result, resourceUrls, baseUrl);

        parseFrames(result, allBlobs, resourceUrls);

        int written = addBlobsToCache(allBlobs);
        logger.verbose("written " + written + " blobs to cache.");

        parseAndCollectCSSResources(allBlobs, baseUrl, resourceUrls);
    }


    private void parseFrames(FrameData result, Map<String, RGridResource> allBlobs, Set<URL> resourceUrls) {
        logger.verbose("handling 'frames' key (level: " + framesLevel.incrementAndGet() + ")");
        for (FrameData frameObj : result.getFrames()) {
            parseScriptResult(frameObj, allBlobs, resourceUrls);
        }
        logger.verbose("done handling 'frames' key (level: " + framesLevel.getAndDecrement() + ")");
    }

    private void parseResourceUrls(FrameData result, Set<URL> resourceUrls, URL baseUrl) {
        List<URL> list = result.getResourceUrls();
        for (URL url : list) {
            try {
                resourceUrls.add(new URL(baseUrl, url.toString()));
            } catch (MalformedURLException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }

        logger.verbose("exit");
    }

    private void parseBlobs(Map<String, RGridResource> allBlobs, Base64 codec, URL baseUrl, List<BlobData> value) {
        //TODO check if empty
        List<BlobData> listOfBlobs = value;
        for (BlobData blob : listOfBlobs) {
            RGridResource resource = parseBlobToGridResource(codec, baseUrl, blob);
            if (!allBlobs.containsKey(resource.getUrl())) {
                allBlobs.put(resource.getUrl(), resource);
            }
        }
    }

    private List<RenderRequest> buildRenderRequests(FrameData result, Map<String, RGridResource> resourceMapping) {

        RGridDom dom = new RGridDom(result.getCdt(), resourceMapping, result.getUrl().toString(), logger, "buildRenderRequests");

        this.dom = dom;

        //Create RG requests
        List<RenderRequest> allRequestsForRG = new ArrayList<>();
        ICheckSettingsInternal checkSettingsInternal = (ICheckSettingsInternal) this.checkSettings;
        ICheckSettingsInternal rcInternal = checkSettingsInternal;


        List<VisualGridSelector> regionSelectorsList = new ArrayList<>();

        for (VisualGridSelector[] regionSelector : this.regionSelectors) {
            regionSelectorsList.addAll(Arrays.asList(regionSelector));
        }

        for (VisualGridTask visualGridTask : this.visualGridTaskList) {

            RenderBrowserInfo browserInfo = visualGridTask.getBrowserInfo();

            String sizeMode = checkSettingsInternal.getSizeMode();

            if (sizeMode.equalsIgnoreCase(VIEWPORT) && forceFullPageScreenshot) {
                sizeMode = FULLPAGE;
            }

            RenderInfo renderInfo = new RenderInfo(browserInfo.getWidth(), browserInfo.getHeight(),
                    sizeMode, rcInternal.getRegion(), rcInternal.getVGTargetSelector(), browserInfo.getEmulationInfo());

            RenderRequest request = new RenderRequest(this.renderingInfo.getResultsUrl(), result.getUrl().toString(), dom,
                    resourceMapping, renderInfo, browserInfo.getPlatform(), browserInfo.getBrowserType(),
                    rcInternal.getScriptHooks(), regionSelectorsList, rcInternal.isSendDom(), visualGridTask);

            allRequestsForRG.add(request);
        }
        return allRequestsForRG;
    }

    private RGridResource parseBlobToGridResource(Base64 codec, URL baseUrl, BlobData blobAsMap) {
        // TODO - handle non-string values (probably empty json objects)
        String contentAsString = blobAsMap.getValue();
        byte[] content = codec.decode(contentAsString);
        String urlAsString = blobAsMap.getUrl().toString();
        try {

            URL url = new URL(baseUrl, urlAsString);
            urlAsString = url.toString();
        } catch (MalformedURLException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);

        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        RGridResource resource = new RGridResource(urlAsString, blobAsMap.getType(), content, logger, "parseBlobToGridResource");
        return resource;
    }

    private void parseAndCollectCSSResources(Map<String, RGridResource> allBlobs, URL baseUrl, Set<URL> resourceUrls) {
        for (RGridResource blob : allBlobs.values()) {
            String contentTypeStr = blob.getContentType();
            String css = getCss(blob.getContent(), contentTypeStr);
            if (css == null) continue;
            parseCSS(css, baseUrl, resourceUrls);
        }
    }

    private String getCss(byte[] contentBytes, String contentTypeStr) {
//        logger.verbose("enter");
        if (contentTypeStr == null) return null;
        if (contentBytes.length == 0) return null;
        String[] parts = contentTypeStr.split(";");
        String charset = "UTF-8";
        for (String part : parts) {
            part = part.trim();
            if (!part.equalsIgnoreCase("text/css")) {
                charset = null;
            } else {
                String[] keyVal = part.split("=");
                if (keyVal.length == 2) {
                    String key = keyVal[0].trim();
                    String val = keyVal[1].trim();
                    if (key.equalsIgnoreCase("charset")) {
                        charset = val.toUpperCase();
                    }
                }
            }
        }

        String css = null;
        if (charset != null) {
            try {
                css = new String(contentBytes, charset);
            } catch (UnsupportedEncodingException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }
//        logger.verbose("exit");
        return css;
    }

    private void parseCSS(String css, URL baseUrl, Set<URL> resourceUrls) {
//        logger.verbose("enter");
        CascadingStyleSheet cascadingStyleSheet = null;
        try {
            cascadingStyleSheet = CSSReader.readFromString(css, ECSSVersion.CSS30);
            if (cascadingStyleSheet == null) {
                logger.verbose("exit - failed to read CSS string");
                return;
            }
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        collectAllImportUris(cascadingStyleSheet, resourceUrls, baseUrl);
        collectAllFontFaceUris(cascadingStyleSheet, resourceUrls, baseUrl);
        collectAllBackgroundImageUris(cascadingStyleSheet, resourceUrls, baseUrl);
//        logger.verbose("exit");
    }

    private void collectAllFontFaceUris(CascadingStyleSheet cascadingStyleSheet, Set<URL> allResourceUris, URL baseUrl) {
        logger.verbose("enter");
        ICommonsList<CSSFontFaceRule> allFontFaceRules = cascadingStyleSheet.getAllFontFaceRules();
        for (CSSFontFaceRule fontFaceRule : allFontFaceRules) {
            getAllResourcesUrisFromDeclarations(allResourceUris, fontFaceRule, "src", baseUrl);
        }
        logger.verbose("exit");
    }

    private void collectAllBackgroundImageUris(CascadingStyleSheet cascadingStyleSheet, Set<URL> allResourceUris, URL baseUrl) {
        logger.verbose("enter");
        ICommonsList<CSSStyleRule> allStyleRules = cascadingStyleSheet.getAllStyleRules();
        for (CSSStyleRule styleRule : allStyleRules) {
            getAllResourcesUrisFromDeclarations(allResourceUris, styleRule, "background", baseUrl);
            getAllResourcesUrisFromDeclarations(allResourceUris, styleRule, "background-image", baseUrl);
        }
        logger.verbose("exit");
    }

    private void collectAllImportUris(CascadingStyleSheet cascadingStyleSheet, Set<URL> allResourceUris, URL baseUrl) {
        logger.verbose("enter");
        ICommonsList<CSSImportRule> allImportRules = cascadingStyleSheet.getAllImportRules();
        for (CSSImportRule importRule : allImportRules) {
            String uri = importRule.getLocation().getURI();
            try {
                URL url = new URL(baseUrl, uri);
                allResourceUris.add(url);
            } catch (MalformedURLException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        }
        logger.verbose("exit");
    }

    private <T extends IHasCSSDeclarations<T>> void getAllResourcesUrisFromDeclarations(Set<URL> allResourceUris, IHasCSSDeclarations<T> rule, String propertyName, URL baseUrl) {
        ICommonsList<CSSDeclaration> sourcesList = rule.getAllDeclarationsOfPropertyName(propertyName);
        for (CSSDeclaration cssDeclaration : sourcesList) {
            CSSExpression cssDeclarationExpression = cssDeclaration.getExpression();
            ICommonsList<ICSSExpressionMember> allExpressionMembers = cssDeclarationExpression.getAllMembers();
            ICommonsList<CSSExpressionMemberTermURI> allUriExpressions = allExpressionMembers.getAllInstanceOf(CSSExpressionMemberTermURI.class);
            for (CSSExpressionMemberTermURI uriExpression : allUriExpressions) {
                try {
                    String uri = uriExpression.getURIString();
                    if (uri.toLowerCase().startsWith("data:")) continue;
                    URL url = new URL(baseUrl, uri);
                    allResourceUris.add(url);
                } catch (MalformedURLException e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                }
            }
        }
    }
//
//    private <T extends IHasCSSDeclarations<T>> void getAllFontResourcesUrisFromDeclarations(Set<URL> allResourceUris, IHasCSSDeclarations<CSSFontFaceRule> rule, String propertyName, URL baseUrl) {
//        ICommonsList<CSSDeclaration> sourcesList = rule.getAllDeclarationsOfPropertyName(propertyName);
//        for (CSSDeclaration cssDeclaration : sourcesList) {
//            CSSExpression cssDeclarationExpression = cssDeclaration.getExpression();
//            ICommonsList<ICSSExpressionMember> allExpressionMembers = cssDeclarationExpression.getAllMembers();
//            ICommonsList<CSSExpressionMemberTermURI> allUriExpressions = allExpressionMembers.getAllInstanceOf(CSSExpressionMemberTermURI.class);
//            for (CSSExpressionMemberTermURI uriExpression : allUriExpressions) {
//                try {
//                    String uri = uriExpression.getURIString();
//                    if (uri.toLowerCase().startsWith("data:")) continue;
//                    URL url = new URL(baseUrl, uri);
//                    allResourceUris.add(url);
//                } catch (MalformedURLException e) {
//                    GeneralUtils.logExceptionStackTrace(logger, e);
//                }
//            }
//        }
//    }
//

    private int addBlobsToCache(Map<String, RGridResource> allBlobs) {
        int written = 0;
        for (RGridResource blob : allBlobs.values()) {
            String url = blob.getUrl();
            synchronized (this.fetchedCacheMap) {
                if (!this.fetchedCacheMap.containsKey(url)) {
                    IResourceFuture resourceFuture = this.eyesConnector.createResourceFuture(blob);
                    logger.verbose("Cache write for url - " + url + " hash:(" + resourceFuture + ")");
                    String contentType = blob.getContentType();
                    if (contentType != null && !contentType.equalsIgnoreCase(CDT)) {
                        this.fetchedCacheMap.put(url, resourceFuture);
                    } else {
                        logger.verbose("tried to store cdt");
                    }
                    written++;
                }
            }
        }
        return written;
    }

    private void fetchAllResources(final Map<String, RGridResource> allBlobs, Set<URL> resourceUrls) throws ExecutionException, InterruptedException {
        logger.verbose("enter");
        List<IResourceFuture> allFetches = new ArrayList<>();

        final Iterator<URL> iterator = resourceUrls.iterator();
        while (iterator.hasNext()) {
            URL link = iterator.next();
            String url = link.toString();
            synchronized (this.fetchedCacheMap) {
                // If resource is already being fetched, remove it from the list, and use the future.
                IResourceFuture fetch = fetchedCacheMap.get(url);
                if (fetch != null) {
                    logger.verbose("cache hit for url " + url);
                    iterator.remove();
                    allFetches.add(fetch);
                    continue;
                }

                // If resource is not being fetched yet (limited guarantee)
                IEyesConnector eyesConnector = this.visualGridTaskList.get(0).getEyesConnector();
                IResourceFuture future = eyesConnector.getResource(link);

                if (!this.fetchedCacheMap.containsKey(url)) {
                    this.fetchedCacheMap.put(url, future);
                    allFetches.add(future);
                    logger.verbose("this.fetchedCacheMap.put(" + url + ")");
                } else {
                    logger.verbose("this.fetchedCacheMap.containsKey(" + url + ")");
                }
            }
        }

        logger.verbose("starting to fetch( " + allFetches.size() + ") fetched resources");
        for (IResourceFuture future : allFetches) {
            RGridResource resource;
            try {
                logger.verbose("trying future.get() for resource " + future.getUrl() + " ...");
                resource = future.get(10, TimeUnit.SECONDS);
                logger.verbose("finishing future.get() for resource " + future.getUrl() + " ...");
            } catch (TimeoutException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
                continue;
            }
            logger.verbose("done getting resource " + future.getUrl());
            try {
                this.debugResourceWriter.write(resource);
            } catch (Exception e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
            logger.verbose("done writing to debugWriter");
            String urlAsString = resource.getUrl();

            removeUrlFromList(urlAsString, resourceUrls);
            allBlobs.put(resource.getUrl(), resource);

            // FIXME - remove this
            String contentType = resource.getContentType();
            String css = null;
            css = getCss(resource.getContent(), contentType);
            logger.verbose("handling " + contentType + " resource from URL: " + urlAsString);
            if (css == null || css.isEmpty() || !contentType.contains("text/css")) continue;
            try {
                parseCSS(css, new URL(urlAsString), resourceUrls);
            } catch (MalformedURLException e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
            }

        }
        logger.verbose("finished fetching(" + allFetches.size() + ")");
        logger.verbose("exit");
    }

    private void removeUrlFromList(String url, Set<URL> resourceUrls) {
        Iterator<URL> iterator = resourceUrls.iterator();
        while (iterator.hasNext()) {
            URL resourceUrl = iterator.next();
            if (resourceUrl.toString().equalsIgnoreCase(url)) {
                iterator.remove();
            }
        }
    }

    private void pollRenderingStatus(Map<RunningRender, RenderRequest> runningRenders) {
        logger.verbose("enter");
        List<String> ids = getRenderIds(runningRenders.keySet());
        logger.verbose("render ids : " + ids);
        List<RenderStatusResults> renderStatusResultsList = null;
        timer.schedule(new TimeoutTask(), HOUR);
        do {
            try {
                renderStatusResultsList = this.eyesConnector.renderStatusById(ids.toArray(new String[0]));
            } catch (Exception e) {
                GeneralUtils.logExceptionStackTrace(logger, e);
                continue;
            }
            if (renderStatusResultsList == null || renderStatusResultsList.isEmpty() || renderStatusResultsList.get(0) == null) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                }
                continue;
            }

            sampleRenderingStatus(runningRenders, ids, renderStatusResultsList);

            if (ids.size() > 0) {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                }
            }

        } while (!ids.isEmpty() && !isTimeElapsed.get());

        if (!ids.isEmpty()) {
            logger.verbose("Render ids that didn't complete in time : ");
            logger.verbose(ids.toString());
        }

        for (String id : ids) {
            for (RunningRender renderedRender : runningRenders.keySet()) {
                String renderId = renderedRender.getRenderId();
                if (renderId.equalsIgnoreCase(id)) {
                    logger.verbose("removing failed render id: " + id);
                    VisualGridTask visualGridTask = runningRenders.get(renderedRender).getVisualGridTask();
                    visualGridTask.setRenderError(id, "too long rendering(rendering exceeded 150 sec)");
                    break;
                }
            }
        }

        ICheckSettingsInternal rcInternal = (ICheckSettingsInternal) checkSettings;
        logger.verbose("marking task as complete: " + rcInternal.getName());
        this.isTaskComplete.set(true);
        this.notifySuccessAllListeners();
        logger.verbose("exit");
    }

    private void sampleRenderingStatus(Map<RunningRender, RenderRequest> runningRenders, List<String> ids, List<RenderStatusResults> renderStatusResultsList) {
        logger.verbose("enter - renderStatusResultsList.size: " + renderStatusResultsList.size());

        for (int i = 0, j = 0; i < renderStatusResultsList.size(); i++) {
            RenderStatusResults renderStatusResults = renderStatusResultsList.get(i);
            if (renderStatusResults == null) {
                continue;
            }

            RenderStatus renderStatus = renderStatusResults.getStatus();
            boolean isRenderedStatus = renderStatus == RenderStatus.RENDERED;
            boolean isErrorStatus = renderStatus == RenderStatus.ERROR;
            logger.verbose("renderStatusResults - " + renderStatusResults);
            if (isRenderedStatus || isErrorStatus) {

                String removedId = ids.remove(j);

                for (RunningRender renderedRender : runningRenders.keySet()) {
                    String renderId = renderedRender.getRenderId();
                    if (renderId.equalsIgnoreCase(removedId)) {
                        VisualGridTask visualGridTask = runningRenders.get(renderedRender).getVisualGridTask();
                        Iterator<VisualGridTask> iterator = openVisualGridTaskList.iterator();
                        while (iterator.hasNext()) {
                            VisualGridTask openVisualGridTask = iterator.next();
                            if (openVisualGridTask.getRunningTest() == visualGridTask.getRunningTest()) {
                                if (isRenderedStatus) {
                                    logger.verbose("setting openVisualGridTask " + openVisualGridTask + " render result: " + renderStatusResults + " to url " + this.result.getUrl());
                                    openVisualGridTask.setRenderResult(renderStatusResults);
                                } else {
                                    logger.verbose("setting openVisualGridTask " + openVisualGridTask + " render error: " + removedId + " to url " + this.result.getUrl());
                                    openVisualGridTask.setRenderError(removedId, renderStatusResults.getError());
                                }
                                iterator.remove();
                            }
                        }
                        logger.verbose("setting visualGridTask " + visualGridTask + " render result: " + renderStatusResults + " to url " + this.result.getUrl());
                        String error = renderStatusResults.getError();
                        if (error != null) {
                            GeneralUtils.logExceptionStackTrace(logger, new Exception(error));
                            visualGridTask.setRenderError(renderId, error);
                        }
                        visualGridTask.setRenderResult(renderStatusResults);
                        break;
                    }
                }
            } else {
                j++;
            }
        }
        logger.verbose("exit");
    }

    public boolean getIsTaskComplete() {
        return isTaskComplete.get();
    }

    public void addListener(RenderTaskListener listener) {
        this.listeners.add(listener);
    }


    private class TimeoutTask extends TimerTask {
        @Override
        public void run() {
            logger.verbose("VG is Timed out!");
            isTimeElapsed.set(true);
        }
    }
}

