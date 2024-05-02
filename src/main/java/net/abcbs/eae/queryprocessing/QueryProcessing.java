package net.abcbs.eae.queryprocessing;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;

import net.abcbs.eae.orchestratorapi.OrchestratorCalls;
import net.abcbs.eae.orchestratorapi.OrchestratorDTO;
import net.abcbs.eae.uipath.database.OrchestratorDtb;
import net.abcbs.eae.uipath.database.OrchestratorDtbDTO;
import net.abcbs.eae.utils.CSVWriting;
import net.abcbs.eae.utils.RetrieveFileProperties;
import net.abcbs.eae.utils.RetrieveIsSharedProperties;
import net.abcbs.issh.util.pub.javabeans.IsSharedJavaBean;

public class QueryProcessing extends IsSharedJavaBean {
    private static final Logger logger = LogManager.getLogger(QueryProcessing.class);
    private OrchestratorDTO orch = new OrchestratorDTO();
    private QueryServiceCall callingObj = new QueryServiceCall();
    private Boolean thisIsHva = false;
        
    public String executeQuery(String dataSource, String schema, String type) { 
        logger.info("Starting QueryProcessing");
        // Instantiate variables
        RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
        RetrieveFileProperties fileProp = new RetrieveFileProperties();
        
        // Get isDB string from Ishared Web admin. If True, queries are gotten from db. If False, queries are gotten from property file
        
        
        
        String isDB = prop.getIsDB();

        // get ITS schema, used if applicable
        String itsSchema = prop.getItsSchema();

        // if query results need to be written to CSV, probably will be changed once query service is running 
        boolean isCSV = false;

        // if query will be sent straight to UiPath Orchestrator we need to convert result to Map
        boolean isOrchApi = false;
        orch.setTenant("Default");
        
        // Strings with Values pulled from the RPAQueryService
        String mappoFacilityMondayA;
        String mappoFacilityMondayB;
        String mappoFacilityMondayC;
        String mappoFacilityOtherA;
        String mappoFacilityOtherB;
        String mappoFacilityOtherC;
        String mappoProfOtherA;
        String mappoProfOtherB;
        String mappoProfOtherC;
        String mappoProfOtherD;
        String mappoProfOtherE;
        String mappoProfMondayA;
        String mappoProfMondayB;
        String mappoProfMondayC;
        String mappoProfMondayD;
        String mappoProfMondayE;
        String hvaModernA;
        String hvaModernB;
        String hvaModernC;
        String hvaModernD;
        String hvaModernE;
        String sqlPropertyString = null;
        

        this.setDbFunctionSelect(true);

        try {
            this.initializeConnection(dataSource, "");

            // Hard coded queries. Will be changed
            if (type.equals("Prof/Monday")) {
                
                if (isDB.equals("True")) {
                    mappoProfMondayA = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOPM/1");
                    mappoProfMondayB = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOPM/2");
                    mappoProfMondayC = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOPM/3");
                    mappoProfMondayD = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOPM/4");    
                    mappoProfMondayE = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOPM/5");
                    callingObj.closeClient();
                    sqlPropertyString = mappoProfMondayA + " " + itsSchema + mappoProfMondayB + itsSchema + mappoProfMondayC + schema + mappoProfMondayD+ schema + mappoProfMondayE;
                }
                else if (isDB.equals("False")) {
                    logger.info("Prof/Monday property file");
                    sqlPropertyString = fileProp.retriveProperty("mappoProfMondayA") + itsSchema + 
                    fileProp.retriveProperty("mappoProfMondayB") + itsSchema +
                    fileProp.retriveProperty("mappoProfMondayC") + schema + 
                    fileProp.retriveProperty("mappoProfMondayD") + schema + 
                    fileProp.retriveProperty("mappoProfMondayE");
                }
               
                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();

                orch.setReference("SCCF_ID");
                orch.setFolder("InternalOperations/HealthAdvantage");
                orch.setQueueName("ProviderPricing_PROF_INPUT");
                isCSV = true;
            } else if (type.equals("Prof/Other")) {
                if (isDB.equals("True")) {
                    mappoProfOtherA = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOP/1");
                    mappoProfOtherB = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOP/2");
                    mappoProfOtherC = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOP/3");
                    mappoProfOtherD = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOP/4");
                    mappoProfOtherE = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOP/5");
                    callingObj.closeClient();
                   sqlPropertyString =  mappoProfOtherA + " " + itsSchema + mappoProfOtherB + itsSchema + mappoProfOtherC + schema + mappoProfOtherD + schema + mappoProfOtherE;    
                }
                else if (isDB.equals("False")) {
                logger.info("Prof/Other property file");
                sqlPropertyString = fileProp.retriveProperty("mappoProfOtherA") + itsSchema +
                            fileProp.retriveProperty("mappoProfOtherB") + itsSchema +
                            fileProp.retriveProperty("mappoProfOtherC") + schema + 
                            fileProp.retriveProperty("mappoProfOtherD") + schema + 
                            fileProp.retriveProperty("mappoProfOtherE");
                }

                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();
    
                orch.setReference("SCCF_ID");
                orch.setFolder("InternalOperations/HealthAdvantage");
                orch.setQueueName("ProviderPricing_PROF_INPUT");
                isCSV = true;
            } else if (type.equals("Fac/Monday")) {
                if (isDB.equals("True")) {
                    mappoFacilityMondayA = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOFM/1");
                    mappoFacilityMondayB = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOFM/2");
                    mappoFacilityMondayC = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPOFM/3");
                    callingObj.closeClient();
                    sqlPropertyString = mappoFacilityMondayA + " " + itsSchema + mappoFacilityMondayB + itsSchema +mappoFacilityMondayC;
                }
                else if (isDB.equals("False")) {
                  logger.info("Fac/Monday property file");
                  sqlPropertyString = fileProp.retriveProperty("mappoFacilityMondayA") + itsSchema + 
                  fileProp.retriveProperty("mappoFacilityMondayB") + itsSchema +
                  fileProp.retriveProperty("mappoFacilityMondayC");
                }
                
                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();

                orch.setReference("SCCF_ID");
                orch.setFolder("InternalOperations/HealthAdvantage");
                orch.setQueueName("ProviderPricing_FAC_INPUT");;
                isCSV = true;
            } else if (type.equals("Fac/Other")) {
                
                if (isDB.equals("True")) {
                    // Adding calls to RPAQueryService 
                    mappoFacilityOtherA = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPO/1");
                    mappoFacilityOtherB = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPO/2");
                    mappoFacilityOtherC = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/HA/MAPPO/3");
                    callingObj.closeClient();
                    sqlPropertyString = mappoFacilityOtherA + " " + itsSchema + mappoFacilityOtherB+ itsSchema + mappoFacilityOtherC;  
                      
                }
                else if (isDB.equals("False")) {
                    logger.info("Fac/Other property file");
                    sqlPropertyString = fileProp.retriveProperty("mappoFacilityOtherA") + itsSchema + 
                            fileProp.retriveProperty("mappoFacilityOtherB") + itsSchema +
                            fileProp.retriveProperty("mappoFacilityOtherC");
                }
                
                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();
                orch.setReference("SCCF_ID");
                orch.setFolder("InternalOperations/HealthAdvantage");
                orch.setQueueName("ProviderPricing_FAC_INPUT");
                isCSV = true;
            } else if (type.equals("HVA/Modern")) {
                
                if (isDB.equals("True")) {
                    thisIsHva = true;
                    sqlPropertyString = callingObj.get(prop.buildQueryServiceURL());
                    callingObj.closeClient();
                }
              
                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();

                orch.setReference("ORIG_CLM");
                orch.setFolder("InternalOperations/BlueAdvantage");
                orch.setQueueName("BANA_HVA_INPUT");
                isOrchApi = true;
            } else if (type.equals("BAA/Diag")) {
                if (isDB.equals("True")) {
                  sqlPropertyString = callingObj.get("https://isshared.abcbs.net/RPAQueryService/resources/eventQuery/Orch/InternalOps/BA/BAAC/1");
                }
                else if (isDB.equals("False")) {
                  sqlPropertyString = fileProp.retriveProperty("baaQuery");
                }
                sqlStatement.append(sqlPropertyString);
                logger.info(sqlStatement);
                preparedStatement = connection.prepareStatement(sqlStatement.toString());
                resultSet = preparedStatement.executeQuery();
                isCSV = true;
            } else if (type.equals("BAA/M2")) {
                logger.info(fileProp.retriveProperty("m2Check"));
                logger.info(fileProp.retriveProperty("m2A"));
                logger.info(fileProp.retriveProperty("m2B"));
                logger.info(fileProp.retriveProperty("m2C"));
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(fileProp.retriveProperty("m2Check"));
                stmt.executeUpdate(fileProp.retriveProperty("m2A"));
                stmt.executeUpdate(fileProp.retriveProperty("m2B"));
                resultSet = stmt.executeQuery(fileProp.retriveProperty("m2C"));
                orch.setReference("CLAIM_NBR");
                orch.setFolder("InternalOperations/BlueAdvantage");
                orch.setQueueName("BA_ITS_M2_REINSTATE");
                resultsToOrchestrator(resultSet);
                stmt.executeUpdate("truncate table M2Temp");
                stmt.executeUpdate("drop table M2Temp");
                stmt.close(); 
            } else if (type.equals("BAA/Covid")) {
                // The covid query is kinda complicated, that's why this one isn't as standard as the other queries
                logger.info(fileProp.retriveProperty("CovidTempTablesCheck"));
                logger.info(fileProp.retriveProperty("CovidA"));
                logger.info(fileProp.retriveProperty("CovidB"));
                logger.info(fileProp.retriveProperty("CovidC"));
                logger.info(fileProp.retriveProperty("CovidD"));
                logger.info(fileProp.retriveProperty("CovidE"));
                Statement stmt = connection.createStatement();
                stmt.executeUpdate(fileProp.retriveProperty("CovidTempTablesCheck"));
                stmt.executeUpdate(fileProp.retriveProperty("CovidA"));
                stmt.executeUpdate(fileProp.retriveProperty("CovidB"));
                stmt.executeUpdate(fileProp.retriveProperty("CovidC"));
                stmt.executeUpdate(fileProp.retriveProperty("CovidD"));
                resultSet = stmt.executeQuery(fileProp.retriveProperty("CovidE"));
                orch.setReference("CLAIM_NBR");
                orch.setFolder("InternalOperations/BlueAdvantage");
                orch.setQueueName("BANA_ITS_INPUT");
                resultsToOrchestrator(resultSet);
                stmt.executeUpdate("truncate table costsharecovid19");
                stmt.executeUpdate("truncate table covid19_banaits");
                stmt.executeUpdate("drop table costsharecovid19");
                stmt.executeUpdate("drop table covid19_banaits");
                stmt.close();
            }
            // determine where query results need to go
            if (isCSV) {
                writeResult(type);
            } else if (isOrchApi) {
                resultsToOrchestrator(resultSet);
            } 
        } catch (Exception e) {
            this.processException(e);
            String stackTrace = ExceptionUtils.getStackTrace(e);
            return "Exception: " + stackTrace; 
        } finally {
            displayResults();
            if (preparedStatement != null) {
                this.closeConnections();
            }
        }
        logger.info("End of QueryProcessing");
        return "Query has been executed"; 
    }

    // writes ResultSet to a file. Currently just CSV but this can be scaled to more types later
    public void writeResult(String type) {
        logger.info("Starting method to write to a CSV file");
        CSVWriting csvWriter = new CSVWriting();
        csvWriter.init(type);
        csvWriter.writeFile(resultSet);
        csvWriter.closeWriter();
    }

    //converts ResultSet to a Map object, for Orchestrator API
    public Map<String, Object> resultsToOrchestrator(ResultSet rs) throws Exception {
        logger.trace("Starting method to load Orchestrator queue");
        RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
        Map<String, Object> map = null;
        OrchestratorCalls call = new OrchestratorCalls(orch.getTenant());
        
        
        //Set is better than list to avoid duplicated elements from the db2 database
        Set<String> refListDb2 = new HashSet<>();
        
        if (rs != null) {
            map = new HashMap<>();
            try {
                // get info about columns
                ResultSetMetaData rsMetaData = rs.getMetaData();
                int columnCount = rsMetaData.getColumnCount();
                // iterate through result set
                while (rs.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        String cName = rsMetaData.getColumnLabel(i);
                        String cValue = rs.getString(cName);
                        //Null case
                        if(rs.wasNull()){
                            map.put(cName,"");
                        
                        } else{
                            map.put(cName, cValue.trim());
                            
                            if (orch.getQueueName().toLowerCase().contains("hva")) {
                                //add the orig claim to the temp list to be the reference for the sql
                                if ("ORIG_CLM".equals(cName)) {
                                    refListDb2.add(cValue.trim());
                                }
                            }
                        }
                    }
                    orch.setContent(map);
                    // add result set data to queue
                    call.addDataToQueue(orch);
                }
                
                /*
                 * Handling HVA duplicated reference 
                 * due void claims. These claims will 
                 * be added one by one into Orchestrator
                 * Power Queue for tracking purposes
                 */
                if(thisIsHva && prop.getHvaPowerFlag()){
                    //getHvaPowerFlag will allow the duplicate reference to be control by a flag true or false
                    String uniqueRefList = String.join(",", refListDb2);
                    //475 = BANA_HVA_INPUT1 645 = BANA_HVA_INPUT
                    String sql = buildReferenceQuery (prop.getQueueDefinitionId(), prop.getmssqlSchema(), uniqueRefList);
                    OrchestratorDtb orchDbt = new OrchestratorDtb();
                    
                    
                    //Add the values from the orchestrator call to the object
                    List<OrchestratorDtbDTO> orchDbtResults = orchDbt.queueItemQuery(sql);
                    if(orchDbtResults.isEmpty()) {
                        //No value matched!
                        logger.info("No values have been found in the Uipath dtb for the required reference values");
                    }
                    else {
                        Map<String, List<Object>> parsedSpecificData = orchDbt.parseSpecificData(orchDbtResults);
                        if(parsedSpecificData.isEmpty()) {
                            logger.info("Parse has failed");
                        }
                        else {
                            orch.setQueueName("BANA_HVA_POWER");
                            int maxValues = parsedSpecificData.values().stream().mapToInt(List::size).max().orElse(0);
                            int keysProcessed = 0;
                            for (int i = 0; i < maxValues; i++) {
                                for (Map.Entry<String, List<Object>> entry : parsedSpecificData.entrySet()) {
                                    String header = entry.getKey();
                                    List<Object> values = entry.getValue();
                                    
                                    if (i < values.size()) {
                                        Object value = values.get(i);
                                        map.put(header, value);
                                        keysProcessed++;
                                    }
                                }
                                if (keysProcessed == parsedSpecificData.size()) {
                                    System.out.println(map);
                                    keysProcessed = 0;
                                    orch.setContent(map);
                                    call.addDataToQueue(orch);
                                }
                            }
                        }
                    }
                }
       
                
            } catch (SQLException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            } finally {
                call.closeClient();
                refListDb2 = null;
            }
        }

        return map;
    }
    
    public String buildReferenceQuery (int queueDefinitionId, String schema, String referenceList) {
        /*
         * TODO: Try to store sqlQuery in a property file like query.mssql = "SELECT id, QueueDefinitionId, Reference, SpecificData FROM uipathdev.dbo.QueueItems WHERE QueueDefinitionId = "{0}" AND Reference in ({1})"
         * Then use Message.Format(sqlQuery,queueDefinitionId,referenceList)
         */
        referenceList = Arrays.stream(referenceList.split(",")).map(value -> "'" + value + "'").collect(Collectors.joining(","));
        String sqlQuery = "SELECT id, QueueDefinitionId, Reference, SpecificData FROM " + schema +".dbo.QueueItems WHERE QueueDefinitionId = " + queueDefinitionId + " AND Reference in ("+ referenceList + ") AND Status <> 0";
        
        //return String.format(sqlQuery, queueDefinitionId, referenceList); placehold is not working
        return sqlQuery;
    }
    
    
}