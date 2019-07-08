package com.applitools.eyes;

import com.applitools.eyes.visualgrid.services.IResourceFuture;
import com.applitools.eyes.visualgrid.model.RGridResource;
import com.applitools.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.brotli.dec.BrotliInputStream;

import javax.ws.rs.core.Response;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ResourceFuture implements IResourceFuture {

    private Future<Response> future;
    private String url;
    private Logger logger;
    private IServerConnector serverConnector;
    private RGridResource rgResource;
    private String userAgent;

    public ResourceFuture(Future<Response> future, String url, Logger logger, IServerConnector serverConnector, String userAgent) {
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
        logger.verbose("entering - " + url);
        synchronized (url) {
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
                    Response response = this.future.get(15, TimeUnit.SECONDS);
                    int status = response.getStatus();
                    List<String> contentLengthHeaders = response.getStringHeaders().get("Content-length");
                    int contentLength = 0;
                    if (contentLengthHeaders != null) {
                        contentLength = Integer.parseInt(contentLengthHeaders.get(0));
                        logger.verbose("Content Length: " + contentLength);
                    }

                    logger.verbose("downloading url - : " + url);

                    if (status == 404) {
                        logger.verbose("Status 404 on url - " + url);
                        break;
                    }

                    if ((status == 200 || status == 201)) {
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
                    } else {
                        retryCount--;
                    }
                } catch (Throwable e) {
                    GeneralUtils.logExceptionStackTrace(logger, e);
                    retryCount--;
                    logger.verbose("Entering retry for - " + url);
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
        logger.verbose("enter -1 this.rgResource: " + this.rgResource);
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

    private byte[] downloadFile(Response response) {

        InputStream inputStream = response.readEntity(InputStream.class);
        Object contentEncoding = response.getHeaders().getFirst("Content-Encoding");
        byte[] bytes = new byte[0];
        try {
            if ("br".equalsIgnoreCase((String) contentEncoding)) {
                inputStream = new BrotliInputStream(inputStream);
            }
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
