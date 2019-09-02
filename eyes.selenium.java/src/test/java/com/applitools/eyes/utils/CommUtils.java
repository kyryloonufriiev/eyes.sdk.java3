package com.applitools.eyes.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;

public class CommUtils {

    public static void putTestResultJsonToSauceLabs(PassedResult passedResult, String sessionId) {
        String sauce_username = System.getenv("SAUCE_USERNAME");
        String sauce_access_key = System.getenv("SAUCE_ACCESS_KEY");
        HttpAuth creds = HttpAuth.basic(sauce_username, sauce_access_key);
        try {
            putJsonRequest("https://saucelabs.com/rest/v1/" + sauce_username + "/jobs/" + sessionId, passedResult, creds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <Tin, Tout> Tout putJsonRequest(String url, Tin data, HttpAuth creds) throws IOException {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        try {
            ObjectMapper jsonMapper = new ObjectMapper();
            String json;
            try {
                json = jsonMapper.writeValueAsString(data);
            } catch (JsonProcessingException e) {
                json = "{}";
                e.printStackTrace();
            }
            HttpPut request = new HttpPut(url);
            if (creds != null) {
                request.addHeader(creds.getHeader());
            }
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            HttpResponse httpResponse = httpClient.execute(request);

            HttpEntity entity = httpResponse.getEntity();

//            byte[] buffer = new byte[1024];
            if (entity != null) {
                InputStream inputStream = entity.getContent();
//                int bytesRead;
//                BufferedInputStream bis = new BufferedInputStream(inputStream);
//                StringBuilder resultJson = new StringBuilder(inputStream.available());
//                while ((bytesRead = bis.read(buffer)) != -1) {
//                    String chunk = new String(buffer, 0, bytesRead);
//                    resultJson.append(chunk);
//                }

                @SuppressWarnings("UnnecessaryLocalVariable")
                Tout result = jsonMapper.readValue(inputStream, new TypeReference<Tout>() {
                });
                ((CloseableHttpResponse) httpResponse).close();
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.close();
        }
        return null;
    }
}
