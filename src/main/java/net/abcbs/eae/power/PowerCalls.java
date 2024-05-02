package net.abcbs.eae.power;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.hc.client5.http.fluent.Request;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;


public class PowerCalls {
    private static final Logger logger = LogManager.getLogger(PowerCalls.class);    
    private CloseableHttpClient httpClient;

    public PowerCalls() {
        logger.info("Starting PowerCalls");
        // initialize the HttpClient object
        logger.trace("Initializing HTTP Client");
        httpClient = initHttpClient();
    }

    // this method initializes and sets the httpClient variable. Customized to trust SSL certificates
    public CloseableHttpClient initHttpClient() {
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
            processExceptions(e);
        }
        return null;
    }
    
    public Document powerRequest(String powerURL, String powerCredentials, String xml) {
        try {
            HttpPost httpPost = new HttpPost(powerURL);
            byte[] encodedAuth = Base64.encodeBase64(
                    powerCredentials.getBytes(StandardCharsets.ISO_8859_1));
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encodedAuth));
            httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "text/xml");

            HttpEntity stringEntity = new StringEntity(xml, ContentType.TEXT_XML);
            
            httpPost.setEntity(stringEntity);

            logger.trace("Sending HTTP request to Power");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            Document doc = null;
            if (response.getCode() == 200) {
                HttpEntity entity = response.getEntity();
                
                if(entity != null){
                String responseText = EntityUtils.toString(entity);  
                
                // the response has some unwanted characters so we need to clean it up
                responseText = responseText.replace("&lt;","<");
                responseText = responseText.replace("&gt;",">");
                responseText = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+"\n"+responseText.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>","");

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputStream is = new ByteArrayInputStream(responseText.getBytes());
                doc = builder.parse(is);
                EntityUtils.consume(entity);}
            }
            
            return doc;
        } catch (Exception e) {
            processExceptions(e);
        } finally {
            closeClient();
        }
        return null;
    }

    // close client when finished
    public void closeClient() {
        try {
            logger.trace("Closing HTTP Client");
            httpClient.close();
        } catch (IOException e) {
            processExceptions(e);
        }
    }

    // get exception stack trace
    public String processExceptions(Exception e) {
        logger.error(ExceptionUtils.getStackTrace(e));
        return ExceptionUtils.getStackTrace(e);
    }
}
