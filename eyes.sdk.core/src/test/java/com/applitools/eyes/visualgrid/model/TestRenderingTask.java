package com.applitools.eyes.visualgrid.model;

import com.applitools.connectivity.ServerConnector;
import com.applitools.eyes.*;
import com.applitools.eyes.utils.ReportingTestSuite;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRenderingTask extends ReportingTestSuite {

    public TestRenderingTask() {
        super.setGroupName("core");
    }

    @Test
    public void testAsyncDownloadResources() throws Exception {
        final ExecutorService service = Executors.newCachedThreadPool();

        // Get a json of uris simulating the real resource uris structure
        File file = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("resource_urls.json")).getFile());
        String jsonString = GeneralUtils.readToEnd(new FileInputStream(file));
        ObjectMapper jsonMapper = new ObjectMapper();
        final Map<String, Map> urls = jsonMapper.readValue(jsonString, Map.class);

        // Arguments for fetchAllResources
        Set<URI> resourceUrls = stringsToUris(urls.keySet());
        final FrameData frameData = mock(FrameData.class);
        when(frameData.getUrl()).thenReturn("");

        // Mocking for RenderingTask
        final Future<?> future = mock(Future.class);
        when(future.get()).thenThrow(new IllegalStateException());
        when(future.get(anyLong(), (TimeUnit) any())).thenThrow(new IllegalStateException());
        UserAgent userAgent = mock(UserAgent.class);
        when(userAgent.getOriginalUserAgentString()).thenReturn("");
        when(frameData.getUserAgent()).thenReturn(userAgent);

        final AtomicInteger counter = new AtomicInteger();
        ServerConnector serverConnector = mock(ServerConnector.class);

        final SyncTaskListener<Map<String, RGridResource>> listener = new SyncTaskListener<>(new Logger(new StdoutLogHandler()), "dom analyzer");
        final DomAnalyzer domAnalyzer = new DomAnalyzer(new Logger(), serverConnector, new NullDebugResourceWriter(),
                frameData, new HashMap<String, RGridResource>(), listener);

        RGridResource cachedResource = mock(RGridResource.class);
        when(cachedResource.getUrl()).thenReturn("12");
        when(cachedResource.getContentType()).thenReturn("");
        when(cachedResource.parse(ArgumentMatchers.<Logger>any(), anyString()))
                .thenReturn(stringsToUris(getInnerMap(urls, "12").keySet()));
        domAnalyzer.cachedResources.put("12", cachedResource);

        // When RenderingTask tries to get a new resource, this task will be submitted to the ExecutorService
        when(serverConnector.downloadResource(ArgumentMatchers.<URI>any(), anyString(), anyString(), ArgumentMatchers.<TaskListener<RGridResource>>any()))
                .thenAnswer(new Answer<Future<?>>() {
            @Override
            public Future<?> answer(final InvocationOnMock invocationOnMock) throws Throwable {
                try {
                    service.submit(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (counter) {
                                try {
                                    // Sleeping so the async tasks will take some time to finish
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    throw new IllegalStateException(e);
                                }

                                counter.getAndIncrement();
                                URI url = invocationOnMock.getArgument(0);
                                Map innerUrls = TestRenderingTask.this.getInnerMap(urls, url.toString());

                                RGridResource resource = mock(RGridResource.class);
                                when(resource.getUrl()).thenReturn(url.toString());
                                when(resource.getContentType()).thenReturn("");
                                try {
                                    when(resource.parse(ArgumentMatchers.<Logger>any(), anyString()))
                                            .thenReturn(stringsToUris(innerUrls.keySet()));
                                } catch (URISyntaxException e) {
                                    throw new IllegalStateException(e);
                                }

                                TaskListener<RGridResource> listener = invocationOnMock.getArgument(3);
                                listener.onComplete(resource);
                            }
                        }
                    });
                    return future;
                } catch (Exception e) {
                    throw new Throwable(e);
                }
            }
        });

        // We call the method which activates the process of collecting resources and wait to see if it ends properly.
        for (URI uri : resourceUrls) {
            domAnalyzer.resourcesToDownload.add(Pair.of(frameData, uri));
        }

        while(!domAnalyzer.run()) {
            Thread.sleep(10);
        }

        Assert.assertEquals(listener.get().size(), 8);
        Assert.assertEquals(counter.get(), 7);
    }

    /**
     * This method searches recursively for a key in a map and returns its value
     */
    private Map getInnerMap(Map<String, Map> outerMap, String key) {
        if (outerMap.containsKey(key)) {
            return outerMap.get(key);
        }

        for (String k : outerMap.keySet()) {
            if (outerMap.get(k).isEmpty()) {
                continue;
            }

            Map result = getInnerMap(outerMap.get(k), key);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * This method converts a collection of string uris to a collection of URIs
     */
    private Set<URI> stringsToUris(Set<String> strUris) throws URISyntaxException {
        Set<URI> uris = new HashSet<>();
        for (String url : strUris) {
            uris.add(new URI(url));
        }

        return uris;
    }
}
