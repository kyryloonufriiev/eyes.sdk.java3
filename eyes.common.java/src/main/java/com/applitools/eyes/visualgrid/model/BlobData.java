package com.applitools.eyes.visualgrid.model;

import java.net.URI;

public class BlobData {
    
    private URI url;

    private String type;

    private String value;

    public BlobData() {
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "BlobData{" +
                "url=" + url +
                '}';
    }
}
