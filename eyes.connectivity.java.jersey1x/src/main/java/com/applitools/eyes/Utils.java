package com.applitools.eyes;

import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String getResponseContentType(ClientResponse response) {
        return getHeaderString(response, "Content-Type");
    }

    public static String getResponseContentEncoding(ClientResponse response) {
        return getHeaderString(response, "Content-Encoding");
    }

    private static String getHeaderString(ClientResponse response, String header) {
        MultivaluedMap<String, String> headers = response.getHeaders();
        String contentType = null;
        for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
            boolean isContentType = entry.getKey().equalsIgnoreCase(header);
            if(isContentType){
                contentType = entry.getValue().get(0);
                break;
            }
        }
        return contentType;
    }
}
