package com.applitools.connectivity;

import com.applitools.IResourceUploadListener;
import com.applitools.connectivity.api.HttpClient;
import com.applitools.connectivity.api.Request;
import com.applitools.connectivity.api.Response;
import com.applitools.connectivity.api.Target;
import com.applitools.eyes.*;
import com.applitools.eyes.visualgrid.PutFuture;
import com.applitools.eyes.visualgrid.ResourceFuture;
import com.applitools.eyes.visualgrid.model.*;
import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.brotli.dec.BrotliInputStream;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;

public class ServerConnector extends RestClient {

    String API_SESSIONS = "api/sessions";
    String CLOSE_BATCH = "api/sessions/batches/%s/close/bypointerid";

    //Rendering Grid
    String RENDER_INFO_PATH = API_SESSIONS + "/renderinfo";
    String RESOURCES_SHA_256 = "/resources/sha256/";
    String RENDER_STATUS = "/render-status";
    String RENDER = "/render";

    private static final String API_PATH = "/api/sessions/running";
    private static final String RENDER_ID = "render-id";

    private String apiKey = null;
    private RenderingInfo renderingInfo;

    /***
     * @param restClient The client for communication
     * @param logger    Logger instance.
     * @param serverUrl The URI of the rest server.
     */
    public ServerConnector(HttpClient restClient, Logger logger, URI serverUrl) {
        super(restClient, logger, serverUrl);
        endPoint = endPoint.path(API_PATH);
    }

    public ServerConnector(HttpClient restClient, Logger logger) {
        this(restClient, logger, GeneralUtils.getServerUrl());
    }

    /**
     * Sets the API key of your applitools Eyes account.
     * @param apiKey The api key to set.
     */
    public void setApiKey(String apiKey) {
        ArgumentGuard.notNull(apiKey, "apiKey");
        this.apiKey = apiKey;
    }

    /**
     * @return The currently set API key or {@code null} if no key is set.
     */
    public String getApiKey() {
        return this.apiKey != null ? this.apiKey : GeneralUtils.getEnvString("APPLITOOLS_API_KEY");
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentId() {
        return this.agentId;
    }

    public void setRenderingInfo(RenderingInfo renderInfo) {
        this.renderingInfo = renderInfo;
    }

    public void updateClient(HttpClient client) {
        super.updateClient(client);
        endPoint.path(API_PATH);
    }

    @Override
    protected Response sendHttpWebRequest(String url, final String method, String... accept) {
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
        }};
        Request request = makeEyesRequest(restClient.target(url), queryParams, accept);
        return request.method(method, null, null);
    }

    /**
     * Starts a new running session in the agent. Based on the given parameters,
     * this running session will either be linked to an existing session, or to
     * a completely new session.
     * @param sessionStartInfo The start parameters for the session.
     * @return RunningSession object which represents the current running
     * session
     * @throws EyesException For invalid status codes, or if response parsing
     *                       failed.
     */
    public RunningSession startSession(SessionStartInfo sessionStartInfo)
            throws EyesException {

        ArgumentGuard.notNull(sessionStartInfo, "sessionStartInfo");
        logger.verbose("Using Jersey1 for REST API calls.");
        String postData;
        try {
            // since the web API requires a root property for this message
            jsonMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
            postData = jsonMapper.writeValueAsString(sessionStartInfo);

            // returning the root property addition back to false (default)
            jsonMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        } catch (IOException e) {
            throw new EyesException("Failed to convert " +
                    "sessionStartInfo into Json string!", e);
        }

        Response response;
        try {
            Map<String, Object> queryParams = new HashMap<String, Object>(){{
                put("apiKey", getApiKey());
            }};
            Request request = makeEyesRequest(endPoint, queryParams);
            response = sendLongRequest(request, HttpMethod.POST, postData, MediaType.APPLICATION_JSON);
        } catch (RuntimeException e) {
            logger.log("startSession(): Server request failed: " + e.getMessage());
            throw e;
        }

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);
        validStatusCodes.add(HttpStatus.SC_CREATED);

        // If this is a new session, we set this flag.
        RunningSession runningSession = parseResponseWithJsonData(response, validStatusCodes, RunningSession.class);
        if (runningSession.getIsNew() == null) {
            runningSession.setIsNew(response.getStatusCode() == HttpStatus.SC_CREATED);
        }

        return runningSession;
    }

    /**
     * Stops the running session.
     * @param runningSession The running session to be stopped.
     * @return TestResults object for the stopped running session
     * @throws EyesException For invalid status codes, or if response parsing
     *                       failed.
     */
    public TestResults stopSession(final RunningSession runningSession,
                                   final boolean isAborted, final boolean save) throws EyesException {
        ArgumentGuard.notNull(runningSession, "runningSession");
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
            put("aborted", String.valueOf(isAborted));
            put("updateBaseline", String.valueOf(save));
        }};
        Request request = makeEyesRequest(endPoint.path(runningSession.getId()), queryParams);
        Response response = sendLongRequest(request, HttpMethod.DELETE, null, null);

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);
        return parseResponseWithJsonData(response, validStatusCodes, TestResults.class);
    }

    public void deleteSession(final TestResults testResults) {
        ArgumentGuard.notNull(testResults, "testResults");
        Target target = restClient.target(serverUrl)
                .path("/api/sessions/batches/")
                .path(testResults.getBatchId())
                .path("/")
                .path(testResults.getId());

        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
            put("AccessToken", testResults.getSecretToken());
        }};

        Request request = makeEyesRequest(target, queryParams);
        request.method(HttpMethod.DELETE, null, null);
    }

    /**
     * Matches the current window (held by the WebDriver) to the expected
     * window.
     * @param runningSession The current agent's running session.
     * @param matchData      Encapsulation of a capture taken from the application.
     * @return The results of the window matching.
     * @throws EyesException For invalid status codes, or response parsing
     *                       failed.
     */
    public MatchResult matchWindow(RunningSession runningSession, MatchWindowData matchData) throws EyesException {
        ArgumentGuard.notNull(runningSession, "runningSession");
        ArgumentGuard.notNull(matchData, "model");

        // Serializing model into JSON (we'll treat it as binary later).
        String jsonData;
        try {
            jsonData = jsonMapper.writeValueAsString(matchData);
        } catch (IOException e) {
            throw new EyesException("Failed to serialize model for matchWindow!", e);
        }

        // Sending the request
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
        }};

        Request request = makeEyesRequest(endPoint.path(runningSession.getId()), queryParams);
        Response response = sendLongRequest(request, HttpMethod.POST, jsonData, MediaType.APPLICATION_JSON);

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>(1);
        validStatusCodes.add(HttpStatus.SC_OK);

        return parseResponseWithJsonData(response, validStatusCodes, MatchResult.class);
    }

    public int uploadData(byte[] bytes, RenderingInfo renderingInfo, String targetUrl, String contentType, String mediaType) {
        Request request = makeEyesRequest(restClient.target(targetUrl), null, mediaType)
                .header("X-Auth-Token", renderingInfo.getAccessToken())
                .header("x-ms-blob-type", "BlockBlob");

        Response response = request.method(HttpMethod.PUT, bytes, contentType);
        int statusCode = response.getStatusCode();
        response.close();
        logger.verbose("Upload Status Code: " + statusCode);
        return statusCode;
    }

    public void downloadString(final URL uri, final boolean isSecondRetry, final IDownloadListener<String> listener) {

    }

    public IResourceFuture downloadResource(final URL url, String userAgent, ResourceFuture resourceFuture) {
        return null;
    }

    public RenderingInfo getRenderInfo() {
        if (renderingInfo != null) {
            return renderingInfo;
        }

        Target target = restClient.target(serverUrl).path(RENDER_INFO_PATH);
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
        }};
        Request request = makeEyesRequest(target, queryParams);
        Response response = sendLongRequest(request, HttpMethod.GET, null, null);

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>(1);
        validStatusCodes.add(HttpStatus.SC_OK);

        renderingInfo = parseResponseWithJsonData(response, validStatusCodes, RenderingInfo.class);
        return renderingInfo;
    }

    public List<RunningRender> render(RenderRequest... renderRequests) {
        return null;
    }

    public boolean renderCheckResource(final RunningRender runningRender, RGridResource resource) {
        ArgumentGuard.notNull(runningRender, "runningRender");
        ArgumentGuard.notNull(resource, "resource");
        this.logger.verbose("called with resource#" + resource.getSha256() + " for render: " + runningRender.getRenderId());

        Target target = restClient.target(renderingInfo.getServiceUrl()).path((RESOURCES_SHA_256) + resource.getSha256());
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("render-id", runningRender.getRenderId());
        }};
        Request request = makeEyesRequest(target, queryParams);
        request.header("X-Auth-Token", renderingInfo.getAccessToken());

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);
        validStatusCodes.add(HttpStatus.SC_NOT_FOUND);

        Response response = request.method(HttpMethod.HEAD, null, null);
        int statusCode = response.getStatusCode();
        if (validStatusCodes.contains(statusCode)) {
            this.logger.verbose("request succeeded");
            return statusCode == HttpStatus.SC_OK;
        }

        throw new EyesException("JBoss ServerConnector.renderCheckResource - unexpected status (" + statusCode + ")");
    }

    public IPutFuture renderPutResource(final RunningRender runningRender, final RGridResource resource, String userAgent, final IResourceUploadListener listener) {
        return null;
    }

    public RenderStatusResults renderStatus(RunningRender runningRender) {
        List<RenderStatusResults> renderStatusResults = renderStatusById(runningRender.getRenderId());
        if (!renderStatusResults.isEmpty()) {
            return renderStatusResults.get(0);
        }
        return null;
    }

    public List<RenderStatusResults> renderStatusById(String... renderIds) {
        try {
            ArgumentGuard.notNull(renderIds, "renderIds");
            this.logger.verbose("called for render: " + Arrays.toString(renderIds));

            Target target = restClient.target(renderingInfo.getServiceUrl()).path((RENDER_STATUS));
            Request request = makeEyesRequest(target, null, MediaType.TEXT_PLAIN);
            request.header("X-Auth-Token", renderingInfo.getAccessToken());

            // Ok, let's create the running session from the response
            List<Integer> validStatusCodes = new ArrayList<>();
            validStatusCodes.add(HttpStatus.SC_OK);
            validStatusCodes.add(HttpStatus.SC_NOT_FOUND);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            try {
                String json = objectMapper.writeValueAsString(renderIds);
                Response response = request.method(HttpMethod.POST, json, MediaType.APPLICATION_JSON);
                if (validStatusCodes.contains(response.getStatusCode())) {
                    this.logger.verbose("request succeeded");
                    RenderStatusResults[] renderStatusResults = parseResponseWithJsonData(response, validStatusCodes, RenderStatusResults[].class);
                    for (RenderStatusResults renderStatusResult : renderStatusResults) {
                        if (renderStatusResult != null && renderStatusResult.getStatus() == RenderStatus.ERROR) {
                            logger.verbose("error on render id - " + renderStatusResult);
                        }
                    }
                    return Arrays.asList(renderStatusResults);
                }
            } catch (JsonProcessingException e) {
                logger.log("exception in render status");
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return null;
    }

    public IResourceFuture createResourceFuture(RGridResource gridResource, String userAgent) {
        return new ResourceFuture(gridResource, logger, this, userAgent);
    }

    public void closeBatch(String batchId) {
        boolean dontCloseBatchesStr = GeneralUtils.getDontCloseBatches();
        if (dontCloseBatchesStr) {
            logger.log("APPLITOOLS_DONT_CLOSE_BATCHES environment variable set to true. Skipping batch close.");
            return;
        }
        ArgumentGuard.notNull(batchId, "batchId");
        this.logger.verbose("called with " + batchId);

        String url = String.format(CLOSE_BATCH, batchId);
        Target target = restClient.target(serverUrl).path(url);
        Map<String, Object> queryParams = new HashMap<String, Object>(){{
            put("apiKey", getApiKey());
        }};
        makeEyesRequest(target, queryParams, (String) null).method(HttpMethod.DELETE, null, null);
        restClient.close();
    }

    public void closeConnector() {
        if (restClient != null) {
            restClient.close();
        }
    }

    public boolean getDontCloseBatches() {
        return "true".equalsIgnoreCase(GeneralUtils.getEnvString("APPLITOOLS_DONT_CLOSE_BATCHES"));
    }

    private byte[] downloadFile(Response response) {
        InputStream inputStream = response.readEntity(InputStream.class);
        String contentEncoding = response.getHeader("Content-Encoding");
        byte[] bytes = new byte[0];
        try {
            if ("br".equalsIgnoreCase(contentEncoding)) {
                inputStream = new BrotliInputStream(inputStream);
            }
            bytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return bytes;
    }
}
