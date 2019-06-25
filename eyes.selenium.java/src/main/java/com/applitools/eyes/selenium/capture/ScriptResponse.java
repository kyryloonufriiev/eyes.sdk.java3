package com.applitools.eyes.selenium.capture;

public class ScriptResponse {
    String value;
    Status status;
    String error;

    public ScriptResponse(String value, Status status, String error) {
        this.value = value;
        this.status = status;
        this.error = error;
    }

    public ScriptResponse() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }



    public enum Status {
        WIP("WIP"),
        ERROR("ERROR"),
        SUCCESS("SUCCESS");

        String status;

        Status(String status) {
            this.status = status;
        }
    }
}
