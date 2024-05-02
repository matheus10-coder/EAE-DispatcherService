package net.abcbs.eae.orchestratorapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
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
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.abcbs.eae.utils.Constants;
import net.abcbs.eae.utils.RetrieveIsSharedProperties;


public class OrchestratorCalls {
    private static final Logger logger = LogManager.getLogger(OrchestratorCalls.class);
    private CloseableHttpClient httpClient;
    private String accessToken;
    private String baseUrl;
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String scope;
    

    public OrchestratorCalls() {
        logger.info("Starting OrchestratorCalls");
        // initialize the HttpClient object
        logger.trace("Initializing HTTP Client");
        httpClient = initHttpClient();
    }

    public OrchestratorCalls(String tenant) {
        logger.info("Starting OrchestratorCalls");
        // initialize the HttpClient object
        logger.trace("Initializing HTTP Client");
        httpClient = initHttpClient();

        // set a few hard coded values, needed for the Orchestrator API (unlikely to change)
        grantType = "client_credentials";
        scope = "OR.Queues";

        // set baseUrl, clientId, clientSecret (obtained from IsSharedProperties)
        getProperties(tenant);

        // set accessToken
        accessToken = getAccessToken();
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
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

    // get data that is needed for API calls, like Client ID and Client Secret
    public void getProperties(String tenant) {
        RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
        baseUrl = prop.getOrchestratorURL();
        clientId = prop.getOrchestratorID(tenant);
        clientSecret = prop.getOrchestratorSecret(tenant);
        
    }

    // get the access token for use in accessing Orchestrator resources.
    public String getAccessToken() {
        String json;
        logger.trace("Getting access token");

        try {
            // Need a POST object, this is the token URL
            HttpPost httpPost = new HttpPost(baseUrl + Constants.ORCH_TOKEN_URL);

            // fill out parameters that the API needs
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("grant_type", grantType));
            formParams.add(new BasicNameValuePair("client_id", clientId));
            formParams.add(new BasicNameValuePair("client_secret", clientSecret));
            formParams.add(new BasicNameValuePair("scope", scope));
            httpPost.setEntity(new UrlEncodedFormEntity(formParams));

            // send the request, get the response
            logger.trace("Sending request for access tokens");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            logger.trace("Requesting complete");
            HttpEntity entity = response.getEntity();
            json = EntityUtils.toString(entity);
            EntityUtils.consume(entity);	
        } catch(IOException | ParseException e) {
            return processExceptions(e);
        }

        // json parsing
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        accessToken = jsonObject.get("access_token").getAsString();
        logger.trace("Access token: {}", accessToken);

        return accessToken;
    }

    // POST request to API to add data to the queue
    public String addDataToQueue(OrchestratorDTO orch) {
        RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
        if (StringUtils.isNotBlank(accessToken)) { 
            // read template file and parse into a JsonObject
            logger.trace("Reading in template JSON");
            try (InputStream inputJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("queueTemplate.json")) {
                Reader readerJson = new InputStreamReader(inputJson, StandardCharsets.UTF_8);
                JsonObject fullJson = JsonParser.parseReader(readerJson).getAsJsonObject();
                logger.trace("Template JSON read");

                // isolate the itemData object that is in the JSON
                // conveniently, any updates to this object also updates the fullJson variable
                JsonObject itemData = fullJson.getAsJsonObject("itemData");

                // now time to fix up the JSON
                // If we ever have a need for these other parameters we can just delete these lines
                itemData.remove("DueDate");
                itemData.remove("DeferDate");
                itemData.remove("RiskSlaDate");
                itemData.remove("Progress");

                // fix up content variable from OrchestratorDTO object
                // this is so we can add the correct JSON to the SpecificContent property
                Gson gson = new Gson();
                JsonElement content;
                
                if(prop.getHvaPowerFlag()) {
                    //hva values will need to be added as string and this eliminate the quotation
                    Type hvaGsonType = new TypeToken<HashMap<String, Object>>(){}.getType(); //change back to string in case doesn't work
                    content = gson.toJsonTree(orch.getContent(), hvaGsonType);
                }
                else {
                    Type gsonType = new TypeToken<HashMap<String, Object>>(){}.getType();
                    content = gson.toJsonTree(orch.getContent(), gsonType);
                }
                

                // add to JSON
                itemData.addProperty("Name", orch.getQueueName());
                itemData.add("SpecificContent", content);
                if (StringUtils.isNotBlank(orch.getPriority())) {
                    itemData.addProperty("Priority", StringUtils.capitalize(orch.getPriority()));
                }
                if (StringUtils.isNotBlank(orch.getReference())) {
                    itemData.addProperty("Reference", orch.getContent().get(orch.getReference()).toString());
                    //add the reference check if so we will send a flag to the original
                } else {
                    itemData.remove("Reference");
                }

                // set up request to Orchestrator
                HttpPost httpPost = new HttpPost(baseUrl + Constants.ORCH_ADD_QUEUE_URL);

                // add headers to the request
                httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                httpPost.addHeader("X-UIPATH-FolderPath", orch.getFolder());

                logger.trace("Completed JSON: {}", fullJson);
                HttpEntity stringEntity = new StringEntity(fullJson.toString(), ContentType.APPLICATION_JSON);
                httpPost.setEntity(stringEntity);

                // send the request, parse the response
                logger.trace("Sending HTTP request to Orchestrator");
                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                String strResponse = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                logger.trace("Response from Orchestrator: {}", strResponse);
                
                return strResponse;
            } catch(Exception e) {
                return processExceptions(e);
            }
        } else {
            logger.warn("Access token is blank");
            return "Access token is blank";
        }
    }
    
    public String addDataToQueueAdditionalReference(OrchestratorDTO orch, String additionalReference) {       
        if (StringUtils.isNotBlank(accessToken)) { 
            // read template file and parse into a JsonObject
            logger.trace("Reading in template JSON");
            try (InputStream inputJson = Thread.currentThread().getContextClassLoader().getResourceAsStream("queueTemplate.json")) {
                Reader readerJson = new InputStreamReader(inputJson, StandardCharsets.UTF_8);
                JsonObject fullJson = JsonParser.parseReader(readerJson).getAsJsonObject();
                logger.trace("Template JSON read");

                // isolate the itemData object that is in the JSON
                // conveniently, any updates to this object also updates the fullJson variable
                JsonObject itemData = fullJson.getAsJsonObject("itemData");

                // now time to fix up the JSON
                // If we ever have a need for these other parameters we can just delete these lines
                itemData.remove("DueDate");
                itemData.remove("DeferDate");
                itemData.remove("RiskSlaDate");
                itemData.remove("Progress");

                // fix up content variable from OrchestratorDTO object
                // this is so we can add the correct JSON to the SpecificContent property
                Gson gson = new Gson();
                Type gsonType = new TypeToken<HashMap<String, Object>>(){}.getType();
                JsonElement content = gson.toJsonTree(orch.getContent(), gsonType);

                // add to JSON
                itemData.addProperty("Name", orch.getQueueName());
                itemData.add("SpecificContent", content);
                if (StringUtils.isNotBlank(orch.getPriority())) {
                    itemData.addProperty("Priority", StringUtils.capitalize(orch.getPriority()));
                }
                if (StringUtils.isNotBlank(orch.getReference())) {
                    itemData.addProperty("Reference", orch.getContent().get(orch.getReference()).toString() + additionalReference);
                } else {
                    itemData.remove("Reference");
                }

                // set up request to Orchestrator
                HttpPost httpPost = new HttpPost(baseUrl + Constants.ORCH_ADD_QUEUE_URL);

                // add headers to the request
                httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                httpPost.addHeader("X-UIPATH-FolderPath", orch.getFolder());

                logger.trace("Completed JSON: {}", fullJson);
                HttpEntity stringEntity = new StringEntity(fullJson.toString(), ContentType.APPLICATION_JSON);
                httpPost.setEntity(stringEntity);

                // send the request, parse the response
                logger.trace("Sending HTTP request to Orchestrator");
                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                String strResponse = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                logger.trace("Response from Orchestrator: {}", strResponse);
                return strResponse;
                
            } catch(Exception e) {
                return processExceptions(e);
            }
        } else {
            logger.warn("Access token is blank");
            return "Access token is blank";
        }
    }
    
    //GET Request to unique reference based on the queue identifier - ORCH API call
    public Map<String, Object> getQueueItemReferenceList(OrchestratorDTO orch, int queueDefinitionId) {
        //ArrayList<String> referenceValues = new ArrayList<>();
        if (StringUtils.isNotBlank(accessToken)) {
            try {
                String endpoint = baseUrl + "odata/QueueItems?$select=Reference&$filter=QueueDefinitionId%20eq%20" + queueDefinitionId;
                HttpGet httpGet = new HttpGet(endpoint);
                
                //Add header to the request
                httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                httpGet.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                httpGet.addHeader("X-UIPATH-FolderPath", orch.getFolder());
                
                //Send the request
                logger.info("Sending the HTTP request to Orchestrator");
                CloseableHttpResponse response = httpClient.execute(httpGet);
                logger.trace("Response from Orchestrator: {}", response.getCode());
                HttpEntity entity = response.getEntity();
                String strResponse = EntityUtils.toString(entity);
                EntityUtils.consume(entity);
                logger.info("Response from Orchestrator: {}", strResponse);
                
                //Parse to Map
                Gson gson = new Gson();
                Type type = new TypeToken<HashMap<String,Object>>(){}.getType();
                return gson.fromJson(strResponse,type);
                
            } catch (Exception e) {
                logger.error("Error executing HTTP request",e);
                return new HashMap<>();
            }
        }
        else {
            logger.warn("Access token is blank");
            return new HashMap<>();
        }
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
