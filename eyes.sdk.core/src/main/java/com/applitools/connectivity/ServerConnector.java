package com.applitools.connectivity;

import com.applitools.connectivity.api.AsyncRequest;
import com.applitools.connectivity.api.AsyncRequestCallback;
import com.applitools.connectivity.api.HttpClient;
import com.applitools.connectivity.api.Response;
import com.applitools.eyes.*;
import com.applitools.eyes.locators.VisualLocatorsData;
import com.applitools.eyes.logging.LogSessionsClientEvents;
import com.applitools.eyes.visualgrid.model.*;
import com.applitools.utils.ArgumentGuard;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpStatus;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class ServerConnector extends UfgConnector {

    static final String CLOSE_BATCH = "api/sessions/batches/%s/close/bypointerid";
    static final String RENDER_STATUS = "/render-status";
    static final String RENDER = "/render";
    static final String RESOURCE_STATUS = "/query/resources-exist";
    static final String RENDERER_INFO = "/job-info";
    static final String MOBILE_DEVICES_PATH = "/app/info/mobile/devices";
    public static final String API_PATH = "/api/sessions/running";
    private static final String LOG_PATH = "/api/sessions/log";

    private Map<String, MobileDeviceInfo> mobileDevicesInfo = null;

    /***
     * @param logger    Logger instance.
     * @param serverUrl The URI of the rest server.
     */
    public ServerConnector(Logger logger, URI serverUrl, int timeout) {
        super(logger, serverUrl, timeout);
    }

    public ServerConnector(Logger logger, URI serverUrl) {
        this(logger, serverUrl, DEFAULT_CLIENT_TIMEOUT);
    }

    public ServerConnector(Logger logger) {
        this(logger, GeneralUtils.getServerUrl());
    }

    public ServerConnector() {
        this(new Logger());
    }

    public void setAgentId(String agentId) {
        logger.log(String.format("Setting agent id: %s", agentId));
        this.agentId = agentId;
    }

    public String getAgentId() {
        return this.agentId;
    }

    /**
     * Sets the current server URL used by the rest client.
     * @param serverUrl The URI of the rest server.
     */
    public void setServerUrl(URI serverUrl) {
        setServerUrlBase(serverUrl);
    }

    public URI getServerUrl() {
        return getServerUrlBase();
    }

    public void updateClient(HttpClient client) {
        restClient = client;
    }

    public void sendLogs(AsyncRequestCallback callback, LogSessionsClientEvents clientEvents) {
        ArgumentGuard.notNull(clientEvents, "clientEvents");
        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl).path(LOG_PATH)
                        .queryParam("apiKey", getApiKey()).asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        try {
            String data = jsonMapper.writeValueAsString(clientEvents);
            sendAsyncRequest(request, HttpMethod.POST, callback, data, MediaType.APPLICATION_JSON);
        } catch (JsonProcessingException e) {
            throw new EyesException("Failed converting client events to string", e);
        }
    }

    /**
     * Starts a new running session in the agent. Based on the given parameters,
     * this running session will either be linked to an existing session, or to
     * a completely new session.
     * @param sessionStartInfo The start parameters for the session.
     * @throws EyesException For invalid status codes, or if response parsing
     *                       failed.
     */
    public void startSession(final TaskListener<RunningSession> listener, SessionStartInfo sessionStartInfo) throws EyesException {
        ArgumentGuard.notNull(sessionStartInfo, "sessionStartInfo");
        logger.verbose("enter");
        initClient();
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

        final AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl).path(API_PATH)
                        .queryParam("apiKey", getApiKey()).asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        AsyncRequestCallback callback = new AsyncRequestCallback() {
            @Override
            public void onComplete(Response response) {
                try {
                    if (response.getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                        RunningSession runningSession = new RunningSession();
                        runningSession.setConcurrencyFull(true);
                        listener.onComplete(runningSession);
                        return;
                    }

                    List<Integer> validStatusCodes = new ArrayList<>();
                    validStatusCodes.add(HttpStatus.SC_OK);
                    validStatusCodes.add(HttpStatus.SC_CREATED);
                    RunningSession runningSession = parseResponseWithJsonData(response, validStatusCodes, new TypeReference<RunningSession>() {});
                    if (runningSession.getIsNew() == null) {
                        runningSession.setIsNew(response.getStatusCode() == HttpStatus.SC_CREATED);
                    }

                    runningSession.setConcurrencyFull(false);
                    listener.onComplete(runningSession);
                } catch (Throwable t) {
                    onFail(t);
                } finally {
                    response.close();
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                GeneralUtils.logExceptionStackTrace(logger, throwable);
                listener.onFail();
            }
        };
        sendLongRequest(request, HttpMethod.POST, callback, postData, MediaType.APPLICATION_JSON);
    }

    /**
     * Stops the running session.
     * @param sessionStopInfo The info of the session to be stopped.
     * @throws EyesException For invalid status codes, or if response parsing
     *                       failed.
     */
    public void stopSession(TaskListener<TestResults> listener, final SessionStopInfo sessionStopInfo) throws EyesException {
        ArgumentGuard.notNull(sessionStopInfo, "sessionStopInfo");
        ArgumentGuard.notNull(sessionStopInfo.getRunningSession(), "runningSession");
        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl).path(API_PATH).path(sessionStopInfo.getRunningSession().getId())
                        .queryParam("apiKey", getApiKey())
                        .queryParam("aborted", String.valueOf(sessionStopInfo.isAborted()))
                        .queryParam("updateBaseline", String.valueOf(sessionStopInfo.shouldSave()))
                        .asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);

        ResponseParsingCallback<TestResults> callback = new ResponseParsingCallback<>(this, validStatusCodes, listener, new TypeReference<TestResults>() {});
        sendLongRequest(request, HttpMethod.DELETE, callback, null, null);
    }

    public void deleteSession(final TaskListener<Void> listener, final TestResults testResults) {
        ArgumentGuard.notNull(testResults, "testResults");
        if (testResults.getId() == null || testResults.getBatchId() == null || testResults.getSecretToken() == null) {
            logger.log("Can't delete session, results are null");
            return;
        }

        initClient();
        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl)
                        .path("/api/sessions/batches/")
                        .path(testResults.getBatchId())
                        .path("/")
                        .path(testResults.getId())
                        .queryParam("apiKey", getApiKey())
                        .queryParam("AccessToken", testResults.getSecretToken())
                        .asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        sendAsyncRequest(request, HttpMethod.DELETE, new AsyncRequestCallback() {
            @Override
            public void onComplete(Response response) {
                listener.onComplete(null);
            }

            @Override
            public void onFail(Throwable throwable) {
                listener.onFail();
            }
        });
    }

    /**
     * Matches the current window (held by the WebDriver) to the expected
     * window.
     * @param matchData      Encapsulation of a capture taken from the application.
     * @throws EyesException For invalid status codes, or response parsing
     *                       failed.
     */
    public void matchWindow(TaskListener<MatchResult> listener, final MatchWindowData matchData) throws EyesException {
        ArgumentGuard.notNull(matchData, "matchData");
        ArgumentGuard.notNull(matchData.getRunningSession(), "runningSession");

        // Serializing model into JSON (we'll treat it as binary later).
        String jsonData;
        try {
            jsonData = jsonMapper.writeValueAsString(matchData);
        } catch (IOException e) {
            throw new EyesException("Failed to serialize model for matchWindow!", e);
        }

        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl).path(API_PATH).path(matchData.getRunningSession().getId())
                        .queryParam("apiKey", getApiKey())
                        .asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        List<Integer> validStatusCodes = new ArrayList<>(1);
        validStatusCodes.add(HttpStatus.SC_OK);
        ResponseParsingCallback<MatchResult> callback = new ResponseParsingCallback<>(this, validStatusCodes, listener, new TypeReference<MatchResult>() {});
        sendLongRequest(request, HttpMethod.POST, callback, jsonData, MediaType.APPLICATION_JSON);
    }

    public void render(final TaskListener<List<RunningRender>> listener, RenderRequest... renderRequests) {
        ArgumentGuard.notNull(renderRequests, "renderRequests");
        this.logger.verbose("called with " + Arrays.toString(renderRequests));
        AsyncRequest request = restClient.target(getRenderInfo().getServiceUrl()).path(RENDER).asyncRequest(MediaType.APPLICATION_JSON);
        request.header("X-Auth-Token", getRenderInfo().getAccessToken());
        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);
        validStatusCodes.add(HttpStatus.SC_NOT_FOUND);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = objectMapper.writeValueAsString(renderRequests);
            ResponseParsingCallback<RunningRender[]> callback = new ResponseParsingCallback<>(this, validStatusCodes, new TaskListener<RunningRender[]>() {
                @Override
                public void onComplete(RunningRender[] runningRenders) {
                    listener.onComplete(runningRenders == null ? null : new ArrayList<>(Arrays.asList(runningRenders)));
                }

                @Override
                public void onFail() {
                    listener.onFail();
                }
            }, new TypeReference<RunningRender[]>() {});
            sendAsyncRequest(request, HttpMethod.POST, callback, json, MediaType.APPLICATION_JSON);
        } catch (JsonProcessingException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
            listener.onComplete(null);
        }
    }

    public void renderStatusById(final TaskListener<List<RenderStatusResults>> listener, String... renderIds) {
        try {
            ArgumentGuard.notNull(renderIds, "renderIds");
            this.logger.verbose("called for render: " + Arrays.toString(renderIds));
            AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
                @Override
                public AsyncRequest build() {
                    return restClient.target(getRenderInfo().getServiceUrl()).path((RENDER_STATUS)).asyncRequest(MediaType.TEXT_PLAIN);
                }
            });
            request.header("X-Auth-Token", getRenderInfo().getAccessToken());

            List<Integer> validStatusCodes = new ArrayList<>();
            validStatusCodes.add(HttpStatus.SC_OK);
            validStatusCodes.add(HttpStatus.SC_NOT_FOUND);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = objectMapper.writeValueAsString(renderIds);

            ResponseParsingCallback<RenderStatusResults[]> callback = new ResponseParsingCallback<>(this, validStatusCodes, new TaskListener<RenderStatusResults[]>() {
                @Override
                public void onComplete(RenderStatusResults[] renderStatusResults) {
                    if (renderStatusResults == null) {
                        listener.onComplete(null);
                        return;
                    }

                    for (RenderStatusResults renderStatusResult : renderStatusResults) {
                        if (renderStatusResult != null && renderStatusResult.getStatus() == RenderStatus.ERROR) {
                            logger.verbose("error on render id - " + renderStatusResult);
                        }
                    }

                    listener.onComplete(new ArrayList<>(Arrays.asList(renderStatusResults)));
                }

                @Override
                public void onFail() {
                    listener.onFail();
                }
            }, new TypeReference<RenderStatusResults[]>() {});
            sendAsyncRequest(request, HttpMethod.POST, callback, json, MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
            listener.onComplete(null);
        }
    }

    public void checkResourceStatus(final TaskListener<Boolean[]> listener, String renderId, HashObject... hashes) {
        try {
            ArgumentGuard.notNull(hashes, "hashes");
            renderId = renderId == null ? "NONE" : renderId;
            AsyncRequest request = restClient.target(getRenderInfo().getServiceUrl()).queryParam("rg_render-id", renderId)
                    .path(RESOURCE_STATUS).asyncRequest(MediaType.APPLICATION_JSON);
            request.header("X-Auth-Token", getRenderInfo().getAccessToken());
            List<Integer> validStatusCodes = new ArrayList<>();
            validStatusCodes.add(HttpStatus.SC_OK);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = objectMapper.writeValueAsString(hashes);
            ResponseParsingCallback<Boolean[]> callback = new ResponseParsingCallback<>(this, validStatusCodes, listener, new TypeReference<Boolean[]>() {});
            sendAsyncRequest(request, HttpMethod.POST, callback, json, MediaType.APPLICATION_JSON);
        } catch (Throwable e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
            listener.onComplete(null);
        }
    }

    public void getJobInfo(TaskListener<JobInfo[]> listener, RenderRequest[] browserInfos) {
        try {
            AsyncRequest request = restClient.target(getRenderInfo().getServiceUrl())
                    .path(RENDERER_INFO).asyncRequest(MediaType.APPLICATION_JSON);
            request.header("X-Auth-Token", getRenderInfo().getAccessToken());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            String json = objectMapper.writeValueAsString(browserInfos);

            List<Integer> validStatusCodes = new ArrayList<>();
            validStatusCodes.add(HttpStatus.SC_OK);

            ResponseParsingCallback<JobInfo[]> callback = new ResponseParsingCallback<>(this, validStatusCodes, listener, new TypeReference<JobInfo[]>() {});
            sendAsyncRequest(request, HttpMethod.POST, callback, json, MediaType.APPLICATION_JSON);
        } catch (Throwable t) {
            GeneralUtils.logExceptionStackTrace(logger, t);
            listener.onFail();
        }
    }

    public void uploadData(final TaskListener<String> listener, final byte[] bytes, final String contentType, final String mediaType) {
        final RenderingInfo renderingInfo = getRenderInfo();
        String targetUrl;
        if (renderingInfo == null || (targetUrl = renderingInfo.getResultsUrl()) == null) {
            listener.onComplete(null);
            return;
        }

        final UUID uuid = UUID.randomUUID();
        final String finalUrl = targetUrl.replace("__random__", uuid.toString());
        logger.verbose("uploading viewport image to " + finalUrl);
        UploadCallback callback = new UploadCallback(listener, this, finalUrl, bytes, contentType, mediaType);
        callback.uploadDataAsync();
    }

    public void uploadImage(final TaskListener<String> listener, final byte[] bytes) {
        uploadData(listener, bytes, "image/png", "image/png");
    }

    public void postLocators(TaskListener<Map<String, List<Region>>> listener, VisualLocatorsData visualLocatorsData) {
        String postData;
        try {
            jsonMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
            postData = jsonMapper.writeValueAsString(visualLocatorsData);
        } catch (IOException e) {
            throw new EyesException("Failed to convert " +
                    "visualLocatorsData into Json string!", e);
        }

        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(serverUrl).path(("api/locators/locate"))
                        .queryParam("apiKey", getApiKey())
                        .asyncRequest(MediaType.APPLICATION_JSON);
            }
        });

        List<Integer> validStatusCodes = new ArrayList<>();
        validStatusCodes.add(HttpStatus.SC_OK);
        ResponseParsingCallback<Map<String, List<Region>>> callback = new ResponseParsingCallback<>(this, validStatusCodes, listener, new TypeReference<Map<String, List<Region>>>() {});
        sendLongRequest(request, HttpMethod.POST, callback, postData, MediaType.APPLICATION_JSON);
    }

    public void closeBatch(String batchId) {
        closeBatch(batchId, serverUrl.toString());
    }

    public void closeBatch(String batchId, String url) {
        SyncTaskListener<Void> listener = new SyncTaskListener<>(logger, "closeBatch");
        closeBatchAsync(listener, batchId, url);
        listener.get();
    }

    public void closeBatchAsync(final TaskListener<Void> listener, String batchId, final String url) {
        ArgumentGuard.notNull(batchId, "batchId");
        this.logger.log("called with " + batchId);

        final String path = String.format(CLOSE_BATCH, batchId);
        initClient();
        AsyncRequest request = makeEyesRequest(new HttpRequestBuilder() {
            @Override
            public AsyncRequest build() {
                return restClient.target(url).path(path)
                        .queryParam("apiKey", getApiKey())
                        .asyncRequest((String) null);
            }
        });

        sendAsyncRequest(request, HttpMethod.DELETE, new AsyncRequestCallback() {
            @Override
            public void onComplete(Response response) {
                try {
                    closeConnector();
                } catch (Throwable t) {
                    GeneralUtils.logExceptionStackTrace(logger, t);
                } finally {
                    listener.onComplete(null);
                }
            }

            @Override
            public void onFail(Throwable throwable) {
                try {
                    GeneralUtils.logExceptionStackTrace(logger, throwable);
                    closeConnector();
                } finally {
                    listener.onFail();
                }
            }
        });
    }

    public void closeConnector() {
        if (restClient != null) {
            restClient.close();
        }
    }

    public Map<String, MobileDeviceInfo> getMobileDevicesInfo() {
        if (mobileDevicesInfo != null) {
            return mobileDevicesInfo;
        }

        try {
            mobileDevicesInfo = getFromServer(new HttpRequestBuilder() {
                @Override
                public AsyncRequest build() {
                    return restClient.target(serverUrl).path(MOBILE_DEVICES_PATH)
                            .queryParam("apiKey", getApiKey()).asyncRequest();
                }
            }, new TypeReference<Map<String, MobileDeviceInfo>>() {});
        } catch (Throwable t) {
            GeneralUtils.logExceptionStackTrace(logger, t);
            mobileDevicesInfo = new HashMap<>();
        }
        return mobileDevicesInfo;
    }
}