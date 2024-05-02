package net.abcbs.eae.uipath.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter.Result;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.abcbs.eae.utils.RetrieveIsSharedProperties;

/**
 * Author: Matheus Ribeiro
 * Description: This object will handle all the connectivity with UiPath Orchestrator Database instances
 *              jdbc will be defined and properties will be imported from the isshared manage prop.
 *              Also, query implementation will be utilized in here as part of our orchestrator database
 *              javabean. Error handling will be performed to avoid any leaks on data security and others
 * Project: Dispatcher Service
 * */
public class OrchestratorDtb {
    private static final Logger logger = LogManager.getLogger(OrchestratorDtb.class);
    private String connectionJdbcUrl; 
    
    
    public OrchestratorDtb() {
        try {
            RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
            connectionJdbcUrl = prop.buildMssqlJdbc();
            
        } catch (Exception e) {
            logger.error("Unable to instantiate OrchestratorDtb: ", e);
            throw e;
        }
    }
    
    /**
     * Method will create the connection with mssql and will expect the query sent
     * to generate a successful resultSet 
     * @throws Exception 
     * @returns List of OrchestratorDtbDTO
     * 
     * */
    public List<OrchestratorDtbDTO> queueItemQuery (String sql) throws Exception{
        List<OrchestratorDtbDTO> dbResponse = null;
        
        Connection conn = null;
        QueryRunner run = new QueryRunner();
        
        logger.info("Setting up database connection");
        logger.trace("JDBC URL - {}", connectionJdbcUrl);

        try {
            //this conn variable below was not working for me until I added this line below
            //apparently you dont't need to do his but it works!
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(connectionJdbcUrl);

            logger.info("Database connection successfully established");

            //capture database results into array of pojos
            ResultSetHandler<List<OrchestratorDtbDTO>> resultSetHandler = new BeanListHandler<OrchestratorDtbDTO>(OrchestratorDtbDTO.class);
            dbResponse = run.query(conn, sql, resultSetHandler);
            
            
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error trying to execute SQL: ", e);
            throw e;
        } finally {
            logger.trace("Closing SQL connections quietly");
            DbUtils.closeQuietly(conn);
        }
        return dbResponse;
    }
    /**
     * Method will parse the complex specific data from the result set
     * and will provide the the map to the user with 
     * the headers and value in a list
     * @returns Map<String, List<String>>
     * 
     * */
    public Map<String, List<Object>> parseSpecificData (List<OrchestratorDtbDTO> resultSet){
       ObjectMapper objectMapper = new ObjectMapper();
       Map<String, List<Object>> parsedSpecificData = new HashMap<>();
       List<String> specificDataList = new ArrayList<>();
       for (Object result : resultSet) {
           if (result instanceof OrchestratorDtbDTO) {
               OrchestratorDtbDTO resultSetDtb = (OrchestratorDtbDTO) result;
               specificDataList.add(resultSetDtb.getSpecificData());
           }
       }
       
       for (String specificData : specificDataList) {
           try {
               JsonNode jsonNode = objectMapper.readTree(specificData);
               JsonNode dynamicProperties = jsonNode.path("DynamicProperties");
               
               dynamicProperties.fieldNames().forEachRemaining(header -> {
                   List<Object> headerValues = parsedSpecificData.computeIfAbsent(header, k -> new ArrayList<>());
                   headerValues.add(dynamicProperties.path(header).toString().replace("\"",""));
               });
           } catch (Exception e) {
               logger.error("Error trying to parse result to Map object. Reason: ", e);
               throw new RuntimeException ("Error trying to parse result to Map object. Reason:", e);
           }
       }
       return parsedSpecificData;
       
    }
      
}
