package com.securer.controller.webHook;

import org.apache.http.client.HttpClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * HttpClient Connection Pool.
 * @author Faraz Ali
 */

//@Component
public class HttpClientConfig {

    private static PoolingHttpClientConnectionManager cm = null;
    private static HttpClient httpClient = null;

    private HttpClientConfig() {

    }

    public static HttpClient getConnection() {

        cm = new PoolingHttpClientConnectionManager();
        setConnectionManager();


        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

        return httpClient;
    }

    private static void setConnectionManager() {
        //max connections to 20
        cm.setMaxTotal(40);
        //set max connection per route to 20
        cm.setDefaultMaxPerRoute(40);
    }

}
