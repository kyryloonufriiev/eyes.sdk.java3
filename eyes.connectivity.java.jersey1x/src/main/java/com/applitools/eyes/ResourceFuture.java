package com.applitools.eyes;

import com.applitools.eyes.visualgrid.model.RGridResource;
import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.utils.GeneralUtils;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResourceFuture implements IResourceFuture {

    private Future<ClientResponse> future;
    private String url;
    private Logger logger;
    private IServerConnector serverConnector;
    private RGridResource rgResource;
    private String userAgent;

    public ResourceFuture(Future<ClientResponse> future, String url, Logger logger, IServerConnector serverConnector, String userAgent) {
        this.future = future;
        this.url = url;
        this.logger = logger;
        this.serverConnector = serverConnector;
        this.userAgent = userAgent;
    }

    public ResourceFuture(RGridResource rgResource, Logger logger, IServerConnector serverConnector, String userAgent) {
        this.url = rgResource.getUrl();
        this.rgResource = rgResource;
        this.logger = logger;
        this.serverConnector = serverConnector;
        this.userAgent = userAgent;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public RGridResource get() throws InterruptedException {
        logger.verbose("entering");
        synchronized (url) {
            logger.verbose("enter - this.rgResource: " + this.rgResource);
            if (this.future == null) {
                try {
                    IResourceFuture newFuture = serverConnector.downloadResource(new URL(this.url), userAgent);
                    this.future = ((ResourceFuture) newFuture).future;
                } catch (MalformedURLException malformedUrlException) {
                    GeneralUtils.logExceptionStackTrace(logger, malformedUrlException);
                }
            }
            int retryCount = 3;
            while (this.rgResource == null && retryCount > 0) {
                try {
                    ClientResponse response = this.future.get(15, TimeUnit.SECONDS);
                    int status = response.getStatus();
                    List<String> contentLengthHeaders = response.getHeaders().get("Content-length");
                    int contentLength = 0;
                    if (contentLengthHeaders != null) {
                        contentLength = Integer.parseInt(contentLengthHeaders.get(0));
                        logger.verbose("Content Length: " + contentLength);
                    }

                    logger.verbose("downloading url - : " + url);

                    if ((status == 200 || status == 201) && (!contentLengthHeaders.isEmpty() && contentLength > 0)) {
                        logger.verbose("response: " + response);
                        byte[] content = downloadFile(response);
                        if (content.length == 0) {
                            throw new Exception("content is empty - url :" + url);
                        }
                        String contentType = Utils.getResponseContentType(response);
                        String contentEncoding = Utils.getResponseContentEncoding(response);
                        if (contentEncoding != null && contentEncoding.contains("gzip")) {
                            content = GeneralUtils.getUnGzipByteArrayOutputStream(content);
                        }
                        rgResource = new RGridResource(url, contentType, content, logger, "ResourceFuture");
                        break;
                    }
                    else{
                        retryCount--;
                    }
                } catch (Throwable e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                    retryCount--;
                    logger.verbose("Entering retry for - "+url);
                    try {
                        Thread.sleep(300);
                        IResourceFuture newFuture = serverConnector.downloadResource(new URL(this.url), userAgent);
                        this.future = ((ResourceFuture) newFuture).future;
                    } catch (MalformedURLException malformedUrlException) {
                        GeneralUtils.logExceptionStackTrace(logger, malformedUrlException);
                    }
                }
            }

        }
        logger.verbose("exit");
        return rgResource;
    }

    @Override
    public RGridResource get(long timeout, TimeUnit unit) {
        try {
            return get();
        } catch (InterruptedException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return null;
    }

    private byte[] downloadFile(ClientResponse response) {
        System.out.println("response.hasEntity = "+response.hasEntity());
        InputStream inputStream = response.getEntityInputStream();
        byte[] bytes = new byte[0];
        try {
            bytes = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            GeneralUtils.logExceptionStackTrace(logger, e);
        }
        return bytes;
    }

    @Override
    public String getUrl() {
        return this.url;
    }
}
