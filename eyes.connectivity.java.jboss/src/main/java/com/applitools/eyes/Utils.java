package com.applitools.eyes;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String getResponseContentType(Response response) {
        return getHeaderString(response, "Content-Type");
    }

    public static String getResponseContentEncoding(Response response) {
        return getHeaderString(response, "Content-Encoding");
    }

    private static String getHeaderString(Response response, String header) {
        MultivaluedMap<String, Object> headers = response.getHeaders();
        String contentType = null;
        for (Map.Entry<String, List<Object>> entry: headers.entrySet()) {
            boolean isContentType = entry.getKey().equalsIgnoreCase(header);
            if(isContentType){
                contentType = (String) entry.getValue().get(0);
                break;
            }
        }
        return contentType;
    }
}
