package com.applitools.eyes.visualgrid.model;

import java.net.URL;
import java.util.List;

public class FrameData
{
    private  List<CdtData> cdt; 

    private URL url;

    private List<URL> resourceUrls;
    
    private List<BlobData> blobs;

    private List<FrameData> frames;

    private URL srcAttr;

    public FrameData() {
    }

    public List<CdtData> getCdt() {
        return cdt;
    }

    public void setCdt(List<CdtData> cdt) {
        this.cdt = cdt;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public List<URL> getResourceUrls() {
        return resourceUrls;
    }

    public void setResourceUrls(List<URL> resourceUrls) {
        this.resourceUrls = resourceUrls;
    }

    public List<BlobData> getBlobs() {
        return blobs;
    }

    public void setBlobs(List<BlobData> blobs) {
        this.blobs = blobs;
    }

    public List<FrameData> getFrames() {
        return frames;
    }

    public void setFrames(List<FrameData> frames) {
        this.frames = frames;
    }

    public URL getSrcAttr() {
        return srcAttr;
    }

    public void setSrcAttr(URL srcAttr) {
        this.srcAttr = srcAttr;
    }
}
