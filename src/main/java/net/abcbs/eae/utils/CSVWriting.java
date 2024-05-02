package net.abcbs.eae.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.ResultSetHelperService;

// class for writing ResultSet output to CSV
public class CSVWriting {
	private static final Logger logger = LogManager.getLogger(CSVWriting.class);
	private ICSVWriter csvWriter;
	private String fileName;
	private String filePath;
	
	public CSVWriting() {
		csvWriter = null;
		fileName = null;
		filePath = null;
	}
	
	// initialize the CSVWriter object with the file path and name
	public void init(String type) {
		try {
			RetrieveIsSharedProperties prop = new RetrieveIsSharedProperties();
			this.fileName = prop.buildFileName(type);
			this.filePath = prop.buildTargetPath();
			            
			ResultSetHelperService service = new ResultSetHelperService();
			service.setDateFormat("MM/dd/yyyy");
			FileWriter writer = new FileWriter(filePath + fileName);
			CSVWriterBuilder builder = new CSVWriterBuilder(writer);
			csvWriter = builder.withResultSetHelper(service).build();
		} catch (Exception e) {
		    logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
	// write to CSV
	public void writeFile(ResultSet resultSet) {
		try {
			csvWriter.writeAll(resultSet, true, true, true);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
	// close the writer
	public void closeWriter() {
		try {
			csvWriter.close();
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
}
