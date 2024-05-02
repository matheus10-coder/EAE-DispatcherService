package net.abcbs.eae.dispatcher.diag;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.abcbs.eae.orchestratorapi.OrchestratorCalls;
import net.abcbs.eae.orchestratorapi.OrchestratorDTO;
import net.abcbs.eae.power.PowerCalls;
import net.abcbs.eae.utils.Constants;
import net.abcbs.eae.utils.RetrieveIsSharedProperties;

public class DiagReviewDispatcher {
    private static final Logger logger = LogManager.getLogger(DiagReviewDispatcher.class);
    private String powerURL;
    private String powerCredentials;
    private String xml;
    private List<DiagReviewDTO> diagArr;
    private List<DiagReviewDTO> diagExceptionArr;
    private OrchestratorDTO orch;
    private String diagSearchName;

    public DiagReviewDispatcher() {
        orch = new OrchestratorDTO();
        diagArr = new ArrayList<>();
        diagExceptionArr = new ArrayList<>();
        RetrieveIsSharedProperties isshprp = new RetrieveIsSharedProperties();
        powerURL = isshprp.getPowerUrl();
        powerCredentials = isshprp.getPowerCredentials("Diag");
        orch.setTenant("Default");
        orch.setFolder(Constants.BLUE_ADVANTAGE_FOLDER);
        orch.setQueueName(isshprp.getOrchQueueName("Diag"));
        diagSearchName = isshprp.getSaveSearchName("Diag");
    }

    public String xmlToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t;
        try {
            t = tf.newTransformer();
            StringWriter writer = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (TransformerException e) {
            logger.error(e);
        }
        return null;
    }

    // prepare the XML to send to Power API
    public void prepSoapEnvelope() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        logger.trace("Parsing template SOAP XML, writing data for save search");
        try {
            // get soap envelope for save_search_bot
            InputStream soapEnv = Thread.currentThread().getContextClassLoader().getResourceAsStream("power-save-search-bot.xml");
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(soapEnv);

            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("soapenv:Body");
            NodeList saveSearch = ((Element)list.item(0)).getElementsByTagName("web:saved_search_bot");
            saveSearch.item(0).getChildNodes().item(1).setTextContent(diagSearchName);
            xml = xmlToString(doc);
            logger.trace("XML Envelope: {}", xml);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error(e);
        }
    }

    public void getPowerClaims() {
        logger.info("Getting claims from POWER");
        PowerCalls power = new PowerCalls();
        prepSoapEnvelope();
        String exceptionQueue;

        try { 
            Document searchResult = power.powerRequest(powerURL, powerCredentials, xml);

            searchResult.getDocumentElement().normalize();
            NodeList records = searchResult.getElementsByTagName("RECORD");

            // parse through RECORD
            for (int i = 0; i < records.getLength(); i++) {
                DiagReviewDTO diagReview = new DiagReviewDTO();
                Node node = records.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    diagReview.setPowerRecord(element.getAttribute("Id"));
                    diagReview.setPowerQueue(element.getElementsByTagName("QUEUES").item(0).getTextContent());
                    
                    if (diagReview.getPowerQueue().contains(("ITS"))) {
                        diagReview.setType("ITS");
                        if(diagReview.getPowerQueue().contains("WM")){
                            exceptionQueue =  Constants.DIAGREVIEW_POWER_EXCEPTION_QUEUE_ITS_WM;
                        }
                        
                        else{
                            exceptionQueue =  Constants.DIAGREVIEW_POWER_EXCEPTION_QUEUE_ITS_TY;
                        }
                    } else {
                        diagReview.setType("Instate");
                        if(diagReview.getPowerQueue().contains("WM")){
                            exceptionQueue =  Constants.DIAGREVIEW_POWER_EXCEPTION_QUEUE_INSTATE_WM;
                        }
                        
                        else{
                            exceptionQueue =  Constants.DIAGREVIEW_POWER_EXCEPTION_QUEUE_INSTATE_TY;
                        }
                    }
                    
                    NodeList field = element.getElementsByTagName("FIELDS").item(0).getChildNodes();
                    // parse through FIELDS
                    for (int j = 0; j < field.getLength(); j++) {
                        Node fieldNode = field.item(j);
                        Element fieldElement = (Element) fieldNode;
                        if (fieldElement.getAttribute("Name").equals("BANA_CLMNO")) {
                            diagReview.setClaimNumber(fieldElement.getTextContent());
                        } else if (fieldElement.getAttribute("Name").equals("PENDCD")) {
                            
                            if((fieldElement.getTextContent().contains("K6") || fieldElement.getTextContent().contains("DT") ||fieldElement.getTextContent().contains("6L")) 
                                    &&  fieldElement.getTextContent().length() < 3){
                                diagReview.setPendCode(fieldElement.getTextContent());
                                diagArr.add(diagReview);
                            }
                            
                            else{
                                diagReview.setPendCode(fieldElement.getTextContent());
                                diagExceptionArr.add(diagReview);
                                sendToExceptionQueue(diagReview.getPowerRecord(), exceptionQueue, diagReview.getPendCode());
                            }     
                        } 
                    }
                }
              
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }
    
    
    // uses put-record-bot.xml to move the Power record to the exception queue
    public void sendToExceptionQueue(String recordId, String queueName, String pendCode) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String putRecordXml;
        String exceptionQueue;
        logger.trace("Parsing template SOAP XML for put_record_bot");


        try {
            // get soap envelope for put_record_bot
            InputStream soapEnv = Thread.currentThread().getContextClassLoader().getResourceAsStream("power-put-record-bot.xml");
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(soapEnv);

            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("soapenv:Body");
            NodeList putRecord = ((Element)list.item(0)).getElementsByTagName("web:put_record_bot");
            putRecord.item(0).getChildNodes().item(1).setTextContent(recordId);
            putRecord.item(0).getChildNodes().item(3).setTextContent(queueName);
            putRecord.item(0).getChildNodes().item(5).setTextContent("Manual Review needed. Claim contain pend code: " + pendCode);
            putRecordXml = xmlToString(doc);
            logger.trace("XML Envelope: {}", putRecordXml);

            // send claims to exception queue
            PowerCalls power = new PowerCalls();
            power.powerRequest(powerURL, powerCredentials, putRecordXml);
        } catch (Exception e) {
            logger.error(e);
        } 
    }
    
    

    // load up Orchestrator queue
    public void toOrchestrator(int limit, String addDateUniqueRef) {
        logger.trace("Starting method to load Orchestrator queue");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<>();
        OrchestratorCalls call = new OrchestratorCalls(orch.getTenant());

        // check to see if there is a limit to the number of records to add. 0 means no limit
        if (limit == 0) {
            limit = diagArr.size();
        }

        try {
            for (int i = 0; i < limit; i++) {
                map = mapper.convertValue(diagArr.get(i), Map.class);
                orch.setContent(map);
                if(StringUtils.isBlank(addDateUniqueRef)) {
                    call.addDataToQueueAdditionalReference(orch, "- " + diagArr.get(i).getPendCode());
                }
                else {
                    call.addDataToQueueAdditionalReference(orch, "- " + diagArr.get(i).getPendCode() + " - " + addDateUniqueRef);
                }
                
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            call.closeClient();
        }
    }

    public String run(int limit, String addUniqueRef) {      
        /*
         * Get the local time & assign to reference
         * To avoid duplicated records and allow retries
         * */
        //LocalDate currentDate = LocalDate.now();
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        orch.setReference("CLAIM_NBR"); 
        try {
            getPowerClaims();
            toOrchestrator(limit,addUniqueRef);
        } catch (Exception e) {
            logger.error(e);
            return "Error when running dispatcher. Check log for stack trace. Exception message: " + e.getMessage();
        }

        return "Diag Review Dispatcher Ran";
    }
}
