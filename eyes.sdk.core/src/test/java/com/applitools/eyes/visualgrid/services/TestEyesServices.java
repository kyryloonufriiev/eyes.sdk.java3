package com.applitools.eyes.visualgrid.services;

import com.applitools.connectivity.MockServerConnector;
import com.applitools.connectivity.ServerConnector;
import com.applitools.eyes.*;
import com.applitools.eyes.visualgrid.model.RenderRequest;
import com.applitools.eyes.visualgrid.model.RenderStatusResults;
import com.applitools.eyes.visualgrid.model.RunningRender;
import org.apache.commons.lang3.tuple.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestEyesServices {
    public static Set<String> getSuccessTasks(ConnectivityService<?, ?> service) {
        Set<String> succeededIds = new HashSet<>();
        for (Pair<String, ?> pair : service.outputQueue) {
            succeededIds.add(pair.getLeft());
        }

        return succeededIds;
    }

    public static Set<String> getFailedTasks(ConnectivityService<?, ?> service) {
        Set<String> failedIds = new HashSet<>();
        for (Pair<String, ?> pair : service.errorQueue) {
            failedIds.add(pair.getLeft());
        }

        return failedIds;
    }

    @Test
    public void testOpenService() {
        final SessionStartInfo openFailedStartInfo = mock(SessionStartInfo.class);
        final SessionStartInfo exceptionStartInfo = mock(SessionStartInfo.class);
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void startSession(final TaskListener<RunningSession> listener, SessionStartInfo sessionStartInfo) {
                if (sessionStartInfo.equals(openFailedStartInfo)) {
                    listener.onFail();
                    return;
                }

                if (sessionStartInfo.equals(exceptionStartInfo)) {
                    throw new EyesException("failed");
                }

                super.startSession(listener, sessionStartInfo);
            }
        };

        OpenService openService = new OpenService(new Logger(), serverConnector, 5);
        openService.TIME_TO_WAIT_FOR_OPEN = 1000;
        openService.addInput("1", mock(SessionStartInfo.class));
        openService.addInput("2", openFailedStartInfo);
        openService.addInput("3", exceptionStartInfo);
        openService.addInput("4", mock(SessionStartInfo.class));
        openService.run();

        Assert.assertEquals(getSuccessTasks(openService), new HashSet<>(Arrays.asList("1", "4")));
        Assert.assertEquals(getFailedTasks(openService), new HashSet<>(Arrays.asList("2", "3")));
    }

    @Test
    public void testRetryWhenServerConcurrencyLimitReached() {
        final AtomicInteger counter = new AtomicInteger(0);
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void startSession(final TaskListener<RunningSession> listener, SessionStartInfo sessionStartInfo) {
                if (counter.getAndIncrement() < 3) {
                    final RunningSession newSession = new RunningSession();
                    newSession.setConcurrencyFull(true);
                    listener.onComplete(newSession);
                    return;
                }

                // Return a valid response from the server
                super.startSession(listener, sessionStartInfo);
            }
        };

        OpenService openService = new OpenService(new Logger(), serverConnector, 5);
        openService.addInput("id", mock(SessionStartInfo.class));
        openService.run();
        Assert.assertEquals(counter.get(), 4);
        Assert.assertEquals(openService.outputQueue.size(), 1);
    }

    @Test
    public void testCheckService() {
        final byte[] uploadFailed = new byte[] {1, 2, 3};
        final byte[] uploadException = new byte[] {4, 5, 6};
        final MatchWindowData matchWindowFailed = mock(MatchWindowData.class);
        final MatchWindowData matchWindowException = mock(MatchWindowData.class);
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void uploadData(final TaskListener<String> listener, final byte[] bytes, final String contentType, final String mediaType) {
                if (Arrays.equals(bytes, uploadFailed)) {
                    listener.onFail();
                    return;
                }

                if (Arrays.equals(bytes, uploadException)) {
                    throw new EyesException("failed");
                }

                super.uploadData(listener, bytes, contentType, mediaType);
            }

            @Override
            public void matchWindow(final TaskListener<MatchResult> listener, MatchWindowData data) {
                if (data.equals(matchWindowFailed)) {
                    listener.onFail();
                    return;
                }

                if (data.equals(matchWindowException)) {
                    throw new EyesException("failed");
                }

                super.matchWindow(listener, data);
            }
        };

        AppOutput appOutput1 = mock(AppOutput.class);
        when(appOutput1.getScreenshotBytes()).thenReturn(uploadFailed);
        MatchWindowData matchWindowUploadFailed = mock(MatchWindowData.class);
        when(matchWindowUploadFailed.getAppOutput()).thenReturn(appOutput1);

        AppOutput appOutput2 = mock(AppOutput.class);
        when(appOutput2.getScreenshotBytes()).thenReturn(uploadException);
        MatchWindowData matchWindowUploadException = mock(MatchWindowData.class);
        when(matchWindowUploadException.getAppOutput()).thenReturn(appOutput2);

        AppOutput appOutput3 = mock(AppOutput.class);
        when(appOutput3.getScreenshotBytes()).thenReturn(new byte[0]);

        MatchWindowData matchWindowSuccess = mock(MatchWindowData.class);
        when(matchWindowSuccess.getAppOutput()).thenReturn(appOutput3);
        when(matchWindowFailed.getAppOutput()).thenReturn(appOutput3);
        when(matchWindowException.getAppOutput()).thenReturn(appOutput3);

        CheckService checkService = new CheckService(new Logger(), serverConnector);
        checkService.addInput("1", matchWindowSuccess);
        checkService.addInput("2", matchWindowFailed);
        checkService.addInput("3", matchWindowException);
        checkService.addInput("4", matchWindowUploadFailed);
        checkService.addInput("5", matchWindowUploadException);
        checkService.run();
        Assert.assertEquals(getSuccessTasks(checkService), new HashSet<>(Collections.singletonList("1")));
        Assert.assertEquals(getFailedTasks(checkService), new HashSet<>(Arrays.asList("2", "3", "4", "5")));
    }

    @Test
    public void testCloseService() {
        final SessionStopInfo openFailedStopInfo = mock(SessionStopInfo.class);
        final SessionStopInfo exceptionStopInfo = mock(SessionStopInfo.class);
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void stopSession(final TaskListener<TestResults> listener, SessionStopInfo sessionStopInfo) {
                if (sessionStopInfo.equals(openFailedStopInfo)) {
                    listener.onFail();
                    return;
                }

                if (sessionStopInfo.equals(exceptionStopInfo)) {
                    throw new EyesException("failed");
                }

                super.stopSession(listener, sessionStopInfo);
            }
        };

        RunningSession runningSession = new RunningSession();
        runningSession.setIsNew(true);
        runningSession.setUrl("");
        CloseService closeService = new CloseService(new Logger(), serverConnector);
        closeService.addInput("1", new SessionStopInfo(runningSession, false, true));
        closeService.addInput("2", openFailedStopInfo);
        closeService.addInput("3", exceptionStopInfo);
        closeService.run();

        Assert.assertEquals(getSuccessTasks(closeService), new HashSet<>(Collections.singletonList("1")));
        Assert.assertEquals(getFailedTasks(closeService), new HashSet<>(Arrays.asList("2", "3")));
    }

    @Test
    public void testRenderService() {
        RenderService renderService = new RenderService(new Logger(), new MockServerConnector());
        renderService.addInput("1", mock(RenderRequest.class));
        renderService.addInput("2", mock(RenderRequest.class));
        renderService.run();
        Assert.assertEquals(getSuccessTasks(renderService), new HashSet<>(Arrays.asList("1", "2")));
    }

    @Test
    public void testRenderServiceRenderFail() {
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void render(final TaskListener<List<RunningRender>> listener, RenderRequest... renderRequests) {
                listener.onFail();
            }
        };

        RenderService renderService = new RenderService(new Logger(), serverConnector);
        renderService.addInput("1", mock(RenderRequest.class));
        renderService.addInput("2", mock(RenderRequest.class));
        renderService.run();
        Assert.assertEquals(getFailedTasks(renderService), new HashSet<>(Arrays.asList("1", "2")));
    }

    @Test
    public void testRenderServiceRenderException() {
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void render(final TaskListener<List<RunningRender>> listener, RenderRequest... renderRequests) {
                throw new EyesException("fail");
            }
        };

        RenderService renderService = new RenderService(new Logger(), serverConnector);
        renderService.addInput("1", mock(RenderRequest.class));
        renderService.addInput("2", mock(RenderRequest.class));
        renderService.run();
        Assert.assertEquals(getFailedTasks(renderService), new HashSet<>(Arrays.asList("1", "2")));
    }

    @Test
    public void testRenderServiceRenderStatusFailed() {
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void renderStatusById(final TaskListener<List<RenderStatusResults>> listener, String... renderIds) {
                listener.onFail();
            }
        };

        RenderService renderService = new RenderService(new Logger(), serverConnector);
        renderService.RENDER_STATUS_POLLING_TIMEOUT = 1000;
        renderService.addInput("1", mock(RenderRequest.class));
        renderService.addInput("2", mock(RenderRequest.class));
        renderService.run();
        Assert.assertEquals(getFailedTasks(renderService), new HashSet<>(Arrays.asList("1", "2")));
    }

    @Test
    public void testRenderServiceRenderStatusException() {
        ServerConnector serverConnector = new MockServerConnector() {
            @Override
            public void renderStatusById(final TaskListener<List<RenderStatusResults>> listener, String... renderIds) {
                throw new EyesException("fail");
            }
        };

        RenderService renderService = new RenderService(new Logger(), serverConnector);
        renderService.addInput("1", mock(RenderRequest.class));
        renderService.addInput("2", mock(RenderRequest.class));
        renderService.run();
        Assert.assertEquals(getFailedTasks(renderService), new HashSet<>(Arrays.asList("1", "2")));
    }
}
