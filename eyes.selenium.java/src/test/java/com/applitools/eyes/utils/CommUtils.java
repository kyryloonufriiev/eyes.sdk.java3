package com.applitools.eyes.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CommUtils {

    public static void putTestResultJsonToSauceLabs(PassedResult passedResult, String sessionId) {
        String sauce_username = System.getenv("SAUCE_USERNAME");
        String sauce_access_key = System.getenv("SAUCE_ACCESS_KEY");
        HttpAuth creds = HttpAuth.basic(sauce_username, sauce_access_key);
        putJson("https://saucelabs.com/rest/v1/" + sauce_username + "/jobs/" + sessionId, passedResult, creds);
    }

    public static <Tout> Tout parseJsonResponse(HttpResponse httpResponse) {

        HttpEntity entity = httpResponse.getEntity();

        if (entity != null) {
            try {
                ObjectMapper jsonMapper = new ObjectMapper();
                InputStream inputStream = null;
                inputStream = entity.getContent();

                @SuppressWarnings("UnnecessaryLocalVariable")
                Tout result = jsonMapper.readValue(inputStream, new TypeReference<Tout>() {
                });
                ((CloseableHttpResponse) httpResponse).close();
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getString(String url) {
        return getString(url, null);
    }

    public static String getString(String url, HttpAuth creds) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet request = new HttpGet(url);
            setCredentials(creds, request);
            HttpResponse httpResponse = httpClient.execute(request);
            HttpEntity entity = httpResponse.getEntity();

            BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
            // Read in all of the post results into a String.
            StringBuilder output = new StringBuilder();
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                output.append(currentLine);
            }

            return  output.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <Tout> Tout getJson(String url) {
        return getJson(url, null);
    }

    public static <Tout> Tout getJson(String url, HttpAuth creds) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpGet request = new HttpGet(url);
            setCredentials(creds, request);
            HttpResponse httpResponse = httpClient.execute(request);
            return parseJsonResponse(httpResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <Tin, Tout> Tout putJson(String url, Tin data, HttpAuth creds) {
        try (CloseableHttpClient httpClient = HttpClients.custom().build()) {
            HttpPut request = new HttpPut(url);
            setCredentials(creds, request);
            String json = createJsonString(data);
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            HttpResponse httpResponse = httpClient.execute(request);
            return parseJsonResponse(httpResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setCredentials(HttpAuth creds, HttpRequestBase request) {
        if (creds != null) {
            request.addHeader(creds.getHeader());
        }
    }

    public static <Tin> String createJsonString(Tin data) {
        ObjectMapper jsonMapper = new ObjectMapper();
        String json;
        try {
            json = jsonMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            json = "{}";
            e.printStackTrace();
        }
        return json;
    }
}
