package net.abcbs.eae.queryprocessing;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QueryServiceCall {
    private CloseableHttpClient httpClient;
    
    public QueryServiceCall() throws IllegalArgumentException{
        // initialize the HttpClient object
        System.out.println("Initialize the HttpClient object");
        httpClient = initHttpClient();
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }


    // this method initializes and sets the httpClient variable. Customized to trust SSL certificates
    public CloseableHttpClient initHttpClient() throws IllegalArgumentException {
          try {
                final SSLContext sslcontext = SSLContexts.custom()
                        .loadTrustMaterial(null, new TrustAllStrategy())
                        .build();
                final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslcontext)
                        .build();
                final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();
                
                return HttpClients.custom()
                        .setConnectionManager(cm)
                        .evictExpiredConnections()
                        .build();

            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                System.out.println(e.getMessage());
            }
            
        return null;
    }


    // get the access token for use in accessing Orchestrator resources.
    public String get(String url) {
        String result = null;
        String response = null; 
        try {
            response = Request.get(url).execute().returnContent().asString();
            System.out.println(response);
            JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
            result = jsonObject.get("query").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    // close client when finished
    public void closeClient() {
        try {
            httpClient.close();
        } catch (IOException e) {
            processExceptions(e);
        }
    }

    // get exception stack trace
    public String processExceptions(Exception e) {
        return ExceptionUtils.getStackTrace(e);
    }
}