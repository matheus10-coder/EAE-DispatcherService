package net.abcbs.eae.utils;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RetrieveFileProperties {
	private static final Logger logger = LogManager.getLogger(RetrieveFileProperties.class);
	private String propertyFileName;

	// default value will be the query property file
	public RetrieveFileProperties() {
		propertyFileName = "QueryProperties/query.properties";
	}
	
	// passing in a fileName will change to a different property file
	public RetrieveFileProperties(String fileName) {
		propertyFileName = fileName;
	}
	
	// read in the property file
	public String retriveProperty(String propertyName) {
		String strProperty = null;
		
		// load properties from properties file
		try (InputStream inputPropertyFile = getClass().getClassLoader().getResourceAsStream(propertyFileName)) {
			Properties property =  new Properties();
			property.load(inputPropertyFile); 
			strProperty = property.getProperty(propertyName);

		} catch(Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			return ExceptionUtils.getStackTrace(e);
	    }
		
		return strProperty; 
	}
	
	// getter for propertyFileName
	public String getPropertyFileName() {
		return propertyFileName;
	}

	// setter for propertyFileName
	public void setPropertyFileName(String propertyFileName) {
		this.propertyFileName = propertyFileName;
	}
	
}
