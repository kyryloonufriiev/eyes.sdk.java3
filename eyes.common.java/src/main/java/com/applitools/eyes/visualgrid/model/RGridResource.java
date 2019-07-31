package com.applitools.eyes.visualgrid.model;

import com.applitools.eyes.Logger;
import com.applitools.utils.GeneralUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class RGridResource {

    @JsonIgnore
    private static final int MAX_RESOURCE_SIZE = 15 * 1024 * 1024;

    @JsonIgnore
    private String url;

    @JsonInclude
    private String contentType;

    @JsonIgnore
    private byte[] content;

    @JsonProperty("hash")
    private String sha256;

    @JsonInclude
    private final String hashFormat = "sha256";

    @JsonIgnore
    private Logger logger;

    @JsonIgnore
    private AtomicBoolean isResourceParsed = new AtomicBoolean(false);

    public String getUrl() {
        return url;
    }

    public RGridResource(String url, String contentType, byte[] content, Logger logger, String msg) {

        this.contentType = contentType;
        this.content = content.length > MAX_RESOURCE_SIZE ? Arrays.copyOf(contentType.getBytes(), MAX_RESOURCE_SIZE) : content;
        this.logger = logger;
        this.sha256 = GeneralUtils.getSha256hash(this.content);
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }


    public byte[] getContent() {
        return content;
    }

    public String getHashFormat() {
        return hashFormat;
    }

    public String getSha256() {
        return sha256;
    }

    @Override
    public String toString() {
        return "RGridResource{" + "url='" + url + '\'' + '}';
    }

    public void setIsResourceParsed(Boolean isResourceParsed) {
        this.isResourceParsed.set(isResourceParsed);
    }

    public boolean isResourceParsed() {
        return isResourceParsed.get();
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

