package net.abcbs.eae.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.abcbs.issh.util.pub.property.IsSharedPropertyObj;

// This class is for retrieving properties set in the IsSharedPropertyService through IsSharedAdminWeb
public class RetrieveIsSharedProperties {
    private static final Logger logger = LogManager.getLogger(RetrieveIsSharedProperties.class);
    private IsSharedPropertyObj isSharedPropertyObj;

    public RetrieveIsSharedProperties() {
        isSharedPropertyObj = new IsSharedPropertyObj(Constants.SYSTEM_NAME, Constants.AUTH_KEY, Constants.AUTH_PASS_PHRASE_DEV);
    }

    // get file path, set in IsSharedAdminWeb/PropertyAdmin
    public String buildTargetPath() {
        String basePath = isSharedPropertyObj.getProperty("reportBasePath");
        
        String envPath = isSharedPropertyObj.getProperty("reportEnvironment");
       
        String appPath = isSharedPropertyObj.getProperty("reportRPAPath");
        
        String processPath = isSharedPropertyObj.getProperty("reportRPAQueryProcessorService");
        
        logger.info("Full file path where CSV files will be dropped: {}{}{}{}", basePath, envPath, appPath, processPath);
        return basePath + envPath + appPath + processPath;
    }

    //Build endpoint for hva query
    public String buildQueryServiceURL() {
        String baseURL = isSharedPropertyObj.getProperty("issharedQueryEndpoint"); 
        
        
        String orchPath = isSharedPropertyObj.getProperty("orchestratorPath");
        
        
        String businessAreaPath = isSharedPropertyObj.getProperty("businessAreaPath");
        
        
        
        String lobPath = isSharedPropertyObj.getProperty("lobPath");
       
        
        
        String processPath = isSharedPropertyObj.getProperty("rpaProcess");
       
        
        
        /**
         * create an array list out of the isshared web property rpaProcess to include all the possible  
         */
        logger.info("Full file path where CSV files will be dropped: {}{}{}{}{}", baseURL, orchPath, businessAreaPath, lobPath, processPath);
        return baseURL + orchPath + businessAreaPath + lobPath+ processPath;
    }

    // get the file name
    public String buildFileName(String type) {
        String fileName = "";
        if (type.contains("Prof")) {
            fileName = isSharedPropertyObj.getProperty("reportMAPPOProf");
        } else if (type.contains("Fac")) {
            fileName = isSharedPropertyObj.getProperty("reportMAPPOFac");
        } else if (type.contains("Diag")) {
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            fileName = isSharedPropertyObj.getProperty("reportBAADiag");
            fileName += date.format(new Date()) + ".csv";
        } else if (type.contains("HVA")) {
            //fileName = isSharedPropertyObj.getProperty("reportHVA");
            fileName = "BA_HVA_INPUT.csv";
        }
        logger.info("File name: {}", fileName);
        return fileName;
    }
    
    public String buildMssqlJdbc() {
        logger.trace("Building JDBC url");
        String jdbcUrl = getmssqlJdbc();
        String server = getmssqlServer();
        String finalJdbcUrl = jdbcUrl.replace("://", "://" + server);
        String username = getmssqlUser();
        String password = getmssqlPassword();
        finalJdbcUrl = finalJdbcUrl + "user=" + username + ";password=" + password;
        return finalJdbcUrl;
    }

    // get the ITS schema
    public String getItsSchema() {
        return isSharedPropertyObj.getProperty("itsSchema");
        
    }

    // get IsDB value. If it is set to True, service get query from DB. If false, it gets value from Property file
    public String getIsDB() {
        logger.info("Is DB: {}", isSharedPropertyObj.getProperty("isDB"));
        return isSharedPropertyObj.getProperty("isDB");
        
    }

    // get Orchestrator URL, for Orchestrator API
    public String getOrchestratorURL() {
        logger.info("Orchestrator URL: {}", isSharedPropertyObj.getProperty("orchURL"));
        return isSharedPropertyObj.getProperty("orchURL");
        
    }

    // get Orchestrator Secret. Based off Tenant. Follow naming convention of tenant + "Secret" i.e. "DefaultSecret"
    public String getOrchestratorSecret(String tenant) {
        logger.info("App secret: {}", isSharedPropertyObj.getProperty(tenant + "Secret"));
        return isSharedPropertyObj.getProperty(tenant + "Secret");
        
    }

    // get Orchestrator ID. Based off Tenant. Follow naming convention of tenant + "ID" i.e. "DefaultID"
    public String getOrchestratorID(String tenant) {
        logger.info("App ID: {}", isSharedPropertyObj.getProperty(tenant + "ID"));
        return isSharedPropertyObj.getProperty(tenant + "ID");
        
    }

    // get POWER URL
    public String getPowerUrl() {
        logger.info("POWER URL: {}", isSharedPropertyObj.getProperty("powerURL"));
        return isSharedPropertyObj.getProperty("powerURL");
        
    }

    // get POWER Credentials
    public String getPowerCredentials(String process) {
        return isSharedPropertyObj.getProperty(process + "Credentials");
        
    }

    // get queue name for a process
    public String getOrchQueueName(String process) {
        return isSharedPropertyObj.getProperty(process + "Queue");
        
    }

    // get POWER save search name for a process
    public String getSaveSearchName(String process) {
        return isSharedPropertyObj.getProperty(process + "Search");
    }
    
    //get mssql jdbc connection string
    public String getmssqlJdbc() {
        return isSharedPropertyObj.getProperty("mssqlJdbcUrl");
    }
    
    //get mssql user name account
    public String getmssqlUser() {
        return isSharedPropertyObj.getProperty("mssqlUser");
    }
    
    //get mssql password
    public String getmssqlPassword() {
        return isSharedPropertyObj.getProperty("mssqlPassword");
    }
    
    //get mssql server
    public String getmssqlServer() {
        return isSharedPropertyObj.getProperty("mssqlServer");
    }
    
    //get mssql server
    public Boolean getHvaPowerFlag() {
        String bool = isSharedPropertyObj.getProperty("hvaPowerflag");
        if(bool.toLowerCase().contains("true")) {
            return true;
        }
        else {
            return false;
        }
         
    }
    //get Queue Definition Id
    public int getQueueDefinitionId() {
        String queueDefId = isSharedPropertyObj.getProperty("hvaQueueDefinitionId");
        if (StringUtils.isNotBlank(queueDefId)) {
            return Integer.valueOf(queueDefId);
        }
        else {
            logger.trace("Queue definition Id has not been identified in isshared property service");
            return 0;
        }
    }
    
    //get mssql uipath db schema
    public String getmssqlSchema() {
        return isSharedPropertyObj.getProperty("mssqlSchema");
    }
    
    
}
