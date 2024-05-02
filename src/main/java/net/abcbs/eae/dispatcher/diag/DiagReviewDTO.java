package net.abcbs.eae.dispatcher.diag;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiagReviewDTO {
    @JsonProperty("CLAIM_NBR")
    private String claimNumber;
    
    @JsonProperty("TYPE")
    private String type;
    
    @JsonProperty("PEND_CODE")
    private String pendCode;
    
    @JsonProperty("POWER_RECORD")
    private String powerRecord;
    
    @JsonProperty("POWER_QUEUE")
    private String powerQueue;

    public String getClaimNumber() {
        return claimNumber;
    }

    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPendCode() {
        return pendCode;
    }

    public void setPendCode(String pendCode) {
        this.pendCode = pendCode;
    }

    public String getPowerRecord() {
        return powerRecord;
    }

    public void setPowerRecord(String powerRecord) {
        this.powerRecord = powerRecord;
    }
    
    public String getPowerQueue() {
        return powerQueue;
    }

    public void setPowerQueue(String powerQueue) {
        this.powerQueue = powerQueue;
    }

    @Override
    public String toString() {
        return "DiagReviewDTO [claimNumber=" + claimNumber + ", type=" + type + ", pendCode=" + pendCode
                + ", powerRecord=" + powerRecord + ", powerQueue=" + powerQueue + "]";
    }
}
