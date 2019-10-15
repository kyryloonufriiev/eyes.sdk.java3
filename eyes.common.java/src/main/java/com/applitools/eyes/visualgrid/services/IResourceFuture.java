package com.applitools.eyes.visualgrid.services;

import com.applitools.eyes.visualgrid.model.RGridResource;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface IResourceFuture extends Future<RGridResource>{
    @Override
    boolean cancel(boolean mayInterruptIfRunning);

    @Override
    boolean isCancelled();

    @Override
    boolean isDone();

    @Override
    RGridResource get() throws InterruptedException, ExecutionException;

    @Override
    RGridResource get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    String getUrl();

    void setResource(RGridResource rgResource);

    void setResponseFuture(Future responseFuture);
}
