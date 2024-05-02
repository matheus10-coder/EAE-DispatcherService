package net.abcbs.eae.orchestratorapi;

import java.util.Map;

public class OrchestratorDTO {
	private Map<String, Object> content;
	private String queueName;
	private String priority;
	private String reference;
	private String tenant;
	private String folder;
	
	public Map<String, Object> getContent() {
		return content;
	}
	public void setContent(Map<String, Object> data) {
		this.content = data;
	}
	public String getQueueName() {
		return queueName;
	}
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getReference() {
		return reference;
	}
	public void setReference(String reference) {
		this.reference = reference;
	}
	public String getTenant() {
		return tenant;
	}
	public void setTenant(String tenant) {
		this.tenant = tenant;
	}
	public String getFolder() {
		return folder;
	}
	public void setFolder(String folder) {
		this.folder = folder;
	}
	public String toString() {
		return "OrchestratorDTO [content=" + content + ", queueName=" + queueName + ", priority=" + priority
				+ ", reference=" + reference + ", tenant=" + tenant + ", folder=" + folder + "]";
	}
	
	
}
