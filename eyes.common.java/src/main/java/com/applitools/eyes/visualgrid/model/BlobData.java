package com.applitools.eyes.visualgrid.model;

import java.net.URI;

public class BlobData {
    
    private String url;

    private String type;

    private String value;

    public BlobData() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
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
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
