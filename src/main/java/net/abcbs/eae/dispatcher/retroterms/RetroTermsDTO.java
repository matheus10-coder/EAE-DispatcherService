package net.abcbs.eae.dispatcher.retroterms;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetroTermsDTO {
    @JsonProperty("Record_ID")
    private String powerRecord;
    
    @JsonProperty("Claim_Number")
    private String claimNumber;
    
    @JsonProperty("Claim_SCCF")
    private String sccfNumber;
    
    @JsonProperty("Power_Queue_Name")
    private String powerQueue;
    
    public RetroTermsDTO(String powerRecord, String claimNumber, String sccfNumber, String powerQueue) {
        this.powerRecord = powerRecord;
        this.claimNumber = claimNumber;
        this.sccfNumber = sccfNumber;
        this.powerQueue = powerQueue;
    }
    
    public RetroTermsDTO() {
        this.powerRecord = "";
        this.claimNumber = "";
        this.sccfNumber = "";
        this.powerQueue = "";
    }

    public String getPowerRecord() {
        return powerRecord;
    }
    
    public void setPowerRecord(String powerRecord) {
        this.powerRecord = powerRecord;
    }
    
    public String getClaimNumber() {
        return claimNumber;
    }
    
    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }
    
    public String getSccfNumber() {
        return sccfNumber;
    }
    
    public void setSccfNumber(String sccfNumber) {
        this.sccfNumber = sccfNumber;
    }
    
    public String getPowerQueue() {
        return powerQueue;
    }
    
    public void setPowerQueue(String powerQueue) {
        this.powerQueue = powerQueue;
    }
    
    @Override
    public String toString() {
        return "RetroTermsDTO [powerRecord=" + powerRecord + ", claimNumber=" + claimNumber + ", sccfNumber="
                + sccfNumber + ", powerQueue=" + powerQueue + "]";
    }
}
