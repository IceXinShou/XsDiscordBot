package com.xs.loader.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static com.xs.loader.util.JsonObjFileManager.streamToString;

public class UrlDataGetter {

    public static String getData(String url) {
        return getData(url, null);
    }

    public static String getDataAuthorization(String url, String authorization) {
        return getData(url, authorization);
    }

    public static String postData(String url, String payload) {
        return postData(url, "application/json; charset=UTF-8", payload, null, null);
    }

    public static String postCookie(String url, String payload, String cookie) {
        return postData(url, "application/json; charset=UTF-8", payload, cookie, null);
    }

    public static String postDataAuthorization(String url, String payload, String authorization) {
        return postData(url, "application/json; charset=UTF-8", payload, null, authorization);
    }

    public static String postDataAuthorization(String url, String contentType, String payload, String authorization) {
        return postData(url, contentType, payload, null, authorization);
    }

    @Nullable
    public static String postData(String url, String contentType, String payload, String cookie, String authorization) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", contentType);
            if (authorization != null)
                conn.setRequestProperty("Authorization", authorization);
            if (cookie != null)
                conn.setRequestProperty("Cookie", cookie);
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            //post
            OutputStream payloadOut = conn.getOutputStream();
            payloadOut.write(payload.getBytes(StandardCharsets.UTF_8));
            payloadOut.flush();

            String result;
            if (conn.getResponseCode() > 399)
                result = streamToString(conn.getErrorStream());
            else
                result = streamToString(conn.getInputStream());
            conn.disconnect();
            return result;
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private static String getData(String urlStr, String authorization) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            if (authorization != null)
                conn.setRequestProperty("Authorization", authorization);
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);

            String result;
            if (conn.getResponseCode() > 399)
                result = streamToString(conn.getErrorStream());
            else
                result = streamToString(conn.getInputStream());
            conn.disconnect();
            return result;
        } catch (IOException e) {
            return null;
        }
    }
}