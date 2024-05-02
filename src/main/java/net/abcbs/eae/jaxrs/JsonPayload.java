package net.abcbs.eae.jaxrs;

import java.util.Arrays;

public class JsonPayload {
	private String message;
	private String queryID;
	private String querySubID;
	private String day;
	private String read;
	private String write;
	private String[] files;

	// Object for the JSON response
	public JsonPayload() {
		message = null;
		queryID = null;
		querySubID = null;
		day = null;
		read = null;
		write = null;
		files = null;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getQueryID() {
		return queryID;
	}
	public void setQueryID(String queryID) {
		this.queryID = queryID;
	}
	public String getQuerySubID() {
		return querySubID;
	}
	public void setQuerySubID(String querySubID) {
		this.querySubID = querySubID;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public String getRead() {
		return read;
	}
	public void setRead(String read) {
		this.read = read;
	}
	public String getWrite() {
		return write;
	}
	public void setWrite(String write) {
		this.write = write;
	}
	public String[] getFiles() {
		return files;
	}
	public void setFiles(String[] files) {
		this.files = files;
	}
	public String toString() {
		return "JsonPayload [message=" + message + ", queryID=" + queryID + ", querySubID=" + querySubID + ", day="
				+ day + ", read=" + read + ", write=" + write + ", files=" + Arrays.toString(files) + "]";
	}
}
