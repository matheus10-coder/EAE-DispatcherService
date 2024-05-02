package net.abcbs.eae.dispatcher.retroterms;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


public class RetroTermsDispatcher {
    private static final Logger logger = LogManager.getLogger(RetroTermsDispatcher.class);
    private String powerURL;
    private String powerCredentials;
    private String powerSearch;
    private String xml;
    private List<RetroTermsDTO> retroTermsArr;
    private OrchestratorDTO orch;

    public RetroTermsDispatcher() {
        orch = new OrchestratorDTO();
        retroTermsArr = new ArrayList<>();
        RetrieveIsSharedProperties isshprp = new RetrieveIsSharedProperties();
        powerURL = isshprp.getPowerUrl();
        //powerURL = "https://abcbspower/power/webservices/record.php";
        powerCredentials = isshprp.getPowerCredentials("RetroTerms");
        powerSearch = isshprp.getSaveSearchName("RetroTerms");
        orch.setTenant("Default");
        orch.setFolder(Constants.BLUE_ADVANTAGE_FOLDER);
        orch.setQueueName(isshprp.getOrchQueueName("RetroTerms"));
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
            throw new RuntimeException(e);
            
        }
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
            saveSearch.item(0).getChildNodes().item(1).setTextContent(powerSearch);
            xml = xmlToString(doc);
            logger.trace("XML Envelope: {}", xml);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }

    }

    public void getPowerClaims() {
        logger.info("Getting claims from POWER");
        PowerCalls power = new PowerCalls();
        prepSoapEnvelope();

        try { 
            Document searchResult = power.powerRequest(powerURL, powerCredentials, xml);

            searchResult.getDocumentElement().normalize();
            NodeList records = searchResult.getElementsByTagName("RECORD");

            // parse through RECORD
            for (int i = 0; i < records.getLength(); i++) {
                RetroTermsDTO retroTerms = new RetroTermsDTO();
                Node node = records.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    retroTerms.setPowerRecord(element.getAttribute("Id"));
                    retroTerms.setPowerQueue(element.getElementsByTagName("QUEUES").item(0).getTextContent());

                    NodeList field = element.getElementsByTagName("FIELDS").item(0).getChildNodes();
                    // parse through FIELDS
                    for (int j = 0; j < field.getLength(); j++) {
                        Node fieldNode = field.item(j);
                        Element fieldElement = (Element) fieldNode;
                        if (fieldElement.getAttribute("Name").equals("BANA_CLMNO")) {
                            retroTerms.setClaimNumber(fieldElement.getTextContent());
                        } else if (fieldElement.getAttribute("Name").equals("SCCF")) {
                            retroTerms.setSccfNumber(fieldElement.getTextContent());
                        }
                    }
                }
                retroTermsArr.add(retroTerms);
            }
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
    }

    // load up Orchestrator queue
    public void toOrchestrator(int limit, String additionalReference) {
        logger.trace("Starting method to load Orchestrator queue");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<>();
        OrchestratorCalls call = new OrchestratorCalls(orch.getTenant());
        String powerRecordId = null;
        String origAdditionalReference = additionalReference; 

        // check to see if there is a limit to the number of records to add. 0 means no limit
        if (limit == 0) {
            limit = retroTermsArr.size();
        }

        try {
            for (int i = 0; i < limit; i++) {
                //setting power record Id
                powerRecordId = retroTermsArr.get(i).getPowerRecord();
                if (retroTermsArr.get(i).getClaimNumber().substring(6,7).toUpperCase().contains("E")) {
                   sendToExceptionQueue(retroTermsArr.get(i).getPowerRecord(), retroTermsArr.get(i).getPowerQueue());
                } else {
                    map = mapper.convertValue(retroTermsArr.get(i), Map.class);
                    orch.setContent(map);
                    //Adding the power record to the reference in orchestrator
                    if(StringUtils.isBlank(origAdditionalReference)) {
                        additionalReference = "_"+powerRecordId;
                    }
                    else {
                        //Cases where we have additional info received from the user for the reference id
                        additionalReference = origAdditionalReference+"_"+powerRecordId;
                    }
                    call.addDataToQueueAdditionalReference(orch, additionalReference);
                
                }
                //cleaning additionalReference resource
                additionalReference = null;
                
            }
        } catch (Exception e) {
            logger.error(e);
            throw e;
        } finally {
            call.closeClient();
        }
    }

    // uses put-record-bot.xml to move the Power record to the exception queue
    public void sendToExceptionQueue(String recordId, String queueName) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String putRecordXml;
        String exceptionQueue;
        logger.trace("Parsing template SOAP XML for put_record_bot");


        try {
            // queue name logic
            if (queueName.contains("WM")) {
                exceptionQueue = Constants.RETRO_TERMS_POWER_EXCEPTION_QUEUE_WM;
            } else if (queueName.contains("TY")) {
                exceptionQueue = Constants.RETRO_TERMS_POWER_EXCEPTION_QUEUE_TY;
            } else {
                logger.error("Queue name is not WM or TY, please review the following Power ticket: {}", recordId);
                throw new Exception("Queue name is not WM or TY, please review the following Power ticket: " + recordId);
            }


            // get soap envelope for put_record_bot
            InputStream soapEnv = Thread.currentThread().getContextClassLoader().getResourceAsStream("power-put-record-bot.xml");
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.parse(soapEnv);

            doc.getDocumentElement().normalize();
            NodeList list = doc.getElementsByTagName("soapenv:Body");
            NodeList putRecord = ((Element)list.item(0)).getElementsByTagName("web:put_record_bot");
            putRecord.item(0).getChildNodes().item(1).setTextContent(recordId);
            putRecord.item(0).getChildNodes().item(3).setTextContent(exceptionQueue);
            putRecord.item(0).getChildNodes().item(5).setTextContent("Claim number contains 'E'");
            putRecordXml = xmlToString(doc);
            logger.trace("XML Envelope: {}", putRecordXml);

            // send claims to exception queue
            PowerCalls power = new PowerCalls();
            power.powerRequest(powerURL, powerCredentials, putRecordXml);
        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException(e);
        } 
    }

    public String run(int limit, String additionalReference) {       
        orch.setReference("Claim_Number");
        try {
            getPowerClaims();
            toOrchestrator(limit, additionalReference);
        } catch (Exception e) {
            logger.error(e);
            return "Error when running dispatcher. Check log for stack trace. Exception message: " + e.getMessage();
        }

        return "RetroTerms Dispatcher Ran";
    }
}
