/*
 * Applitools SDK for Selenium integration.
 */
package com.applitools.eyes;

import com.applitools.IResourceUploadListener;
import com.applitools.eyes.visualgrid.model.*;
import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.message.GZipEncoder;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides an API for communication with the Applitools agent
 */
@SuppressWarnings("WeakerAccess")
public class ServerConnector extends RestClient
        implements IServerConnector {

    private String apiKey = null;
    private RenderingInfo renderingInfo;

    /***
     * @param logger A logger instance.
     * @param serverUrl The URI of the Eyes server.
     */
    public ServerConnector(Logger logger, URI serverUrl) {
        super(logger, serverUrl, TIMEOUT);
        endPoint = endPoint.path(API_PATH);
    }

    /***
     * @param logger A logger instance.
     */
    public ServerConnector(Logger logger) {
        this(logger, GeneralUtils.getDefaultServerUrl());
    }

    /***
     * @param serverUrl The URI of the Eyes server.
     */
    public ServerConnector(URI serverUrl) {
        this(null, serverUrl);
    }

    public ServerConnector() {
        this((Logger) null);
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
        return this.apiKey != null ? this.apiKey : System.getenv("APPLITOOLS_API_KEY");
    }

    /**
     * Sets the proxy settings to be used by the rest client.
     * @param proxySettings The proxy settings to be used by the rest client.
     *                      If {@code null} then no proxy is set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setProxy(AbstractProxySettings proxySettings) {
        setProxyBase(proxySettings);
        // After the server is updated we must make sure the endpoint refers
        // to the correct path.
        endPoint = endPoint.path(API_PATH);
    }

    /**
     * @return The current proxy settings used by the rest client,
     * or {@code null} if no proxy is set.
     */
    @SuppressWarnings("UnusedDeclaration")
    public AbstractProxySettings getProxy() {
        return getProxyBase();
    }

    /**
     * Sets the current server URL used by the rest client.
     * @param serverUrl The URI of the rest server.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setServerUrl(URI serverUrl) {
        setServerUrlBase(serverUrl);
        // After the server is updated we must make sure the endpoint refers
        // to the correct path.
        endPoint = endPoint.path(API_PATH);
    }

    /**
     * @return The URI of the eyes server.
     */
    @SuppressWarnings("UnusedDeclaration")
    public URI getServerUrl() {
        return getServerUrlBase();
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

        logger.verbose("Using Jersey2 for REST API calls.");

        String postData;
        Response response;
        int statusCode;
        List<Integer> validStatusCodes;
        boolean isNewSession;
        RunningSession runningSession;
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

        try {
            Invocation.Builder request = endPoint.queryParam("apiKey", getApiKey()).
                    request(MediaType.APPLICATION_JSON);
            response = sendWithRetry(HttpMethod.POST, request, Entity.json(postData), null);
        } catch (RuntimeException e) {
            logger.log("Server request failed: " + e.getMessage());
            throw e;
        }

        // Ok, let's create the running session from the response
        validStatusCodes = new ArrayList<>();
        validStatusCodes.add(Response.Status.OK.getStatusCode());
        validStatusCodes.add(Response.Status.CREATED.getStatusCode());

        runningSession = parseResponseWithJsonData(response, validStatusCodes,
                RunningSession.class);

        // If this is a new session, we set this flag.
        statusCode = response.getStatus();
        isNewSession = (statusCode == Response.Status.CREATED.getStatusCode());
        runningSession.setIsNewSession(isNewSession);

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
                                   final boolean isAborted, final boolean save)
            throws EyesException {

        ArgumentGuard.notNull(runningSession, "runningSession");

        final String sessionId = runningSession.getId();
        Response response;
        List<Integer> validStatusCodes;
        TestResults result;

        HttpMethodCall delete = new HttpMethodCall() {
            public Response call() {

                String currentTime = GeneralUtils.toRfc1123(
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")));

                // Building the request
                Invocation.Builder invocationBuilder = endPoint.path(sessionId)
                        .queryParam("apiKey", getApiKey())
                        .queryParam("aborted", String.valueOf(isAborted))
                        .queryParam("updateBaseline", String.valueOf(save))
                        .request(MediaType.APPLICATION_JSON)
                        .header("Eyes-Expect", "202-accepted")
                        .header("Eyes-Date", currentTime);

                // Actually perform the method call and return the result
                return invocationBuilder.delete();
            }
        };

        response = sendLongRequest(delete, "stopSession");

        // Ok, let's create the running session from the response
        validStatusCodes = new ArrayList<>();
        validStatusCodes.add(Response.Status.OK.getStatusCode());

        result = parseResponseWithJsonData(response, validStatusCodes,
                TestResults.class);
        return result;
    }

    @Override
    public void deleteSession(TestResults testResults) {
        ArgumentGuard.notNull(testResults, "testResults");

        Invocation.Builder invocationBuilder = restClient.target(serverUrl)
                .path("/api/sessions/batches/")
                .path(testResults.getBatchId())
                .path("/")
                .path(testResults.getId())
                .queryParam("apiKey", getApiKey())
                .queryParam("AccessToken", testResults.getSecretToken())
                .request(MediaType.APPLICATION_JSON);

        @SuppressWarnings("unused")
        Response response = invocationBuilder.delete();
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
    public MatchResult matchWindow(RunningSession runningSession,
                                   MatchWindowData matchData)
            throws EyesException {

        ArgumentGuard.notNull(runningSession, "runningSession");
        ArgumentGuard.notNull(matchData, "model");

        Response response;
        List<Integer> validStatusCodes;
        MatchResult result;
        String jsonData;

        // since we rather not add an empty "tag" param
        WebTarget runningSessionsEndpoint =
                endPoint.path(runningSession.getId());

        // Serializing model into JSON (we'll treat it as binary later).
        // IMPORTANT This serializes everything EXCEPT for the screenshot (which
        // we'll add later).
        try {
            jsonData = jsonMapper.writeValueAsString(matchData);
        } catch (IOException e) {
            throw new EyesException("Failed to serialize model for matchWindow!",
                    e);
        }

        // Convert the JSON to binary.
        byte[] jsonBytes;
        ByteArrayOutputStream jsonToBytesConverter = new ByteArrayOutputStream();
        try {
            jsonToBytesConverter.write(
                    jsonData.getBytes(DEFAULT_CHARSET_NAME));
            jsonToBytesConverter.flush();
            jsonBytes = jsonToBytesConverter.toByteArray();
        } catch (IOException e) {
            throw new EyesException("Failed create binary model from JSON!", e);
        }

        // Getting the screenshot's bytes (notice this can be either
        // compressed/uncompressed form).
        byte[] screenshot = Base64.decodeBase64(
                matchData.getAppOutput().getScreenshot64());

        // Ok, let's create the request model
        ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
        DataOutputStream requestDos = new DataOutputStream(requestOutputStream);
        byte[] requestData;
        try {
            requestDos.writeInt(jsonBytes.length);
            requestDos.flush();
            requestOutputStream.write(jsonBytes);
            if (screenshot != null) {
                requestOutputStream.write(screenshot);
            }
            requestOutputStream.flush();

            // Ok, get the model bytes
            requestData = requestOutputStream.toByteArray();

            // Release the streams
            requestDos.close();
        } catch (IOException e) {
            throw new EyesException("Failed send check window request!", e);
        }

        // Sending the request
        Invocation.Builder request = runningSessionsEndpoint.queryParam("apiKey", getApiKey()).
                request(MediaType.APPLICATION_JSON);
        response = sendWithRetry(HttpMethod.POST, request, Entity.entity(requestData,
                MediaType.APPLICATION_OCTET_STREAM), null);

        // Ok, let's create the running session from the response
        validStatusCodes = new ArrayList<>(1);
        validStatusCodes.add(Response.Status.OK.getStatusCode());

        result = parseResponseWithJsonData(response, validStatusCodes,
                MatchResult.class);

        return result;

    }

    @Override
    public void downloadString(final URL uri, final boolean isSecondRetry, final IDownloadListener<String> listener) {

        Client client = ClientBuilder.newBuilder().build();

        WebTarget target = client.target(uri.toString());

        Invocation.Builder request = target.request(MediaType.WILDCARD);

        logger.verbose("Firing async GET");

        request.async().get(new InvocationCallback<String>() {
            @Override
            public void completed(String response) {
                listener.onDownloadComplete(response, null);
            }

            @Override
            public void failed(Throwable throwable) {
                // on fail
                if (!isSecondRetry) {
                    logger.verbose("Async GET failed - entering retry");
                    downloadString(uri, true, listener);
                } else {
                    listener.onDownloadFailed();
                }
            }
        });

    }

    @Override
    public IResourceFuture downloadResource(final URL url, String userAgent) {
        Client client = RestClient.buildRestClient(getTimeout(), getProxy());

        WebTarget target = client.target(url.toString());

        Invocation.Builder request = target.request(MediaType.WILDCARD);

        request.header("User-Agent", userAgent);

        Future<Response> future = request.async().get();

        @SuppressWarnings("UnnecessaryLocalVariable")
        IResourceFuture newFuture = new ResourceFuture(future, url.toString(), logger, this, userAgent);
        return newFuture;
    }

    @Override
    public String postDomSnapshot(String domJson) {

        restClient.register(GZipEncoder.class);

        WebTarget target = restClient.target(serverUrl).path((RUNNING_DATA_PATH)).queryParam("apiKey", getApiKey());
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON);

        byte[] resultStream = GeneralUtils.getGzipByteArrayOutputStream(domJson);

        Response response = sendWithRetry(HttpMethod.POST, request, Entity.entity(resultStream,
                MediaType.APPLICATION_OCTET_STREAM), null);
        @SuppressWarnings("UnnecessaryLocalVariable")
        String entity = response.getHeaderString("Location");
        return entity;
    }

    private Response sendWithRetry(String method, Invocation.Builder request, Entity entity, AtomicInteger retiresCounter) {

        if (retiresCounter == null) {

            retiresCounter = new AtomicInteger(0);

        }

        Response response = null;
        try {
            switch (method) {

                case HttpMethod.POST:
                    response = request.post(entity);
                    break;
                case HttpMethod.PUT:
                    response = request.put(entity);
            }

            return response;
        } catch (Exception e) {

            GeneralUtils.logExceptionStackTrace(logger, e);
            try {

                Thread.sleep(THREAD_SLEEP_MILLIS);

            } catch (InterruptedException e1) {
                GeneralUtils.logExceptionStackTrace(logger, e1);
            }

            if (retiresCounter.incrementAndGet() < NUM_OF_RETRIES) {

                return sendWithRetry(method, request, entity, retiresCounter);
            } else {
                throw e;
            }
        }

    }


    @Override
    public RenderingInfo getRenderInfo() {

        String apiKey = getApiKey();
        WebTarget target = restClient.target(serverUrl).path((RENDER_INFO_PATH)).queryParam("apiKey", apiKey);
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON);

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(Response.Status.OK.getStatusCode());

        Response response = request.get();
        renderingInfo = parseResponseWithJsonData(response, validStatusCodes, RenderingInfo.class);
        return renderingInfo;
    }

    @Override
    public List<RunningRender> render(RenderRequest... renderRequests) {
        ArgumentGuard.notNull(renderRequests, "renderRequests");
        this.logger.verbose("called with " + Arrays.toString(renderRequests));

        WebTarget target = restClient.target(renderingInfo.getServiceUrl()).path((RENDER));
        if (renderRequests.length > 1) {
            target.matrixParam("render-id", (Object) renderRequests);
        } else {
            target.queryParam("render-id", (Object) renderRequests);
        }
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON);
        request.header("X-Auth-Token", renderingInfo.getAccessToken());

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(Response.Status.OK.getStatusCode());
        validStatusCodes.add(Response.Status.NOT_FOUND.getStatusCode());

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = objectMapper.writeValueAsString(renderRequests);
            Response response = request.post(Entity.json(json));
            if (validStatusCodes.contains(response.getStatus())) {
                RunningRender[] runningRenders = parseResponseWithJsonData(response, validStatusCodes, RunningRender[].class);
                return Arrays.asList(runningRenders);
            }
            throw new EyesException("Jersey2 ServerConnector.render - unexpected status (" + response.getStatus() + "), msg (" + response.readEntity(String.class) + ")");
        } catch (JsonProcessingException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }

        return null;
    }

    @Override
    public boolean renderCheckResource(RunningRender runningRender, RGridResource resource) {

        ArgumentGuard.notNull(runningRender, "runningRender");
        ArgumentGuard.notNull(resource, "resource");
        // eslint-disable-next-line max-len
        this.logger.verbose("called with resource#" + resource.getSha256() + " for render: " + runningRender.getRenderId());

        WebTarget target = restClient.target(renderingInfo.getServiceUrl()).path((RESOURCES_SHA_256) + resource.getSha256()).queryParam("render-id", runningRender.getRenderId());
        Invocation.Builder request = target.request(MediaType.APPLICATION_JSON);
        request.header("X-Auth-Token", renderingInfo.getAccessToken());

        // Ok, let's create the running session from the response
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(Response.Status.OK.getStatusCode());
        validStatusCodes.add(Response.Status.NOT_FOUND.getStatusCode());

        Response response = request.head();
        if (validStatusCodes.contains(response.getStatus())) {
            this.logger.verbose("request succeeded");
            return response.getStatus() == Response.Status.OK.getStatusCode();
        }

        throw new EyesException("Jersey2 ServerConnector.renderCheckResource - unexpected status (" + response.getStatus() + ")");
    }

    @Override
    public IPutFuture renderPutResource(final RunningRender runningRender, final RGridResource resource, String userAgent, final IResourceUploadListener listener) {
        ArgumentGuard.notNull(runningRender, "runningRender");
        ArgumentGuard.notNull(resource, "resource");
        byte[] content = resource.getContent();
        ArgumentGuard.notNull(content, "resource.getContent()");

        String hash = resource.getSha256();
        String renderId = runningRender.getRenderId();
        logger.verbose("resource hash:" + hash + " ; url: " + resource.getUrl() + " ; render id: " + renderId);

        WebTarget target = restClient.target(renderingInfo.getServiceUrl())
                .path(RESOURCES_SHA_256 + hash)
                .queryParam("render-id", renderId);

        String contentType = resource.getContentType();
        Invocation.Builder request = target.request(contentType);
        request.header("X-Auth-Token", renderingInfo.getAccessToken());
        request.header("User-Agent", userAgent);
        Entity entity = null;
        if (contentType != null) {
            entity = Entity.entity(content, contentType);

        }
        else{
            entity = Entity.entity(content, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        }
        final Future<Response> future = request.async().put(entity);
        logger.verbose("future created.");
        PutFuture putFuture = new PutFuture(future, resource, runningRender, this, logger, userAgent);
        return putFuture;
    }


    @Override
    public RenderStatusResults renderStatus(RunningRender runningRender) {
        return null;
    }

    @Override
    public List<RenderStatusResults> renderStatusById(String... renderIds) {

        try {
            ArgumentGuard.notNull(renderIds, "renderIds");
            this.logger.verbose("called for render: " + Arrays.toString(renderIds));

            WebTarget target = restClient.target(renderingInfo.getServiceUrl()).path((RENDER_STATUS));
            Invocation.Builder request = target.request(MediaType.TEXT_PLAIN);
            request.header("X-Auth-Token", renderingInfo.getAccessToken());

            // Ok, let's create the running session from the response
            List<Integer> validStatusCodes = new ArrayList<>();
            validStatusCodes.add(Response.Status.OK.getStatusCode());
            validStatusCodes.add(Response.Status.NOT_FOUND.getStatusCode());

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            try {
                String json = objectMapper.writeValueAsString(renderIds);
                Entity<String> entity = Entity.entity(json, MediaType.APPLICATION_JSON);
                Response response = request.post(entity);
                if (validStatusCodes.contains(response.getStatus())) {
                    this.logger.verbose("request succeeded");
                    RenderStatusResults[] renderStatusResults = parseResponseWithJsonData(response, validStatusCodes, RenderStatusResults[].class);
                    for (int i = 0; i < renderStatusResults.length; i++) {
                        RenderStatusResults renderStatusResult = renderStatusResults[i];
                        if(renderStatusResult != null && renderStatusResult.getStatus() == RenderStatus.ERROR){
                            logger.verbose("error on render id - "+renderStatusResult);
                        }
                    }
                    return Arrays.asList(renderStatusResults);
                }
            } catch (JsonProcessingException e) {
                logger.log("exception in render status");
                GeneralUtils.logExceptionStackTrace(logger, e);
            }
            return null;
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return null;

    }

    @Override
    public IResourceFuture createResourceFuture(RGridResource gridResource, String userAgent) {
        return new ResourceFuture(gridResource, logger, this, userAgent);

    }

    @Override
    public void setRenderingInfo(RenderingInfo renderInfo) {
        this.renderingInfo = renderInfo;
    }
}
