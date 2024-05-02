package net.abcbs.eae.dispatcher;

public class DispatcherDTO {
    private String process;
    private int limit;
    private String additionalReference;
    
    public DispatcherDTO() {
        process = "";
    }
    
    public DispatcherDTO(String process) {
        this.process = process;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getAdditionalReference() {
        return additionalReference;
    }

    public void setAdditionalReference(String additionalReference) {
        this.additionalReference = additionalReference;
    }

    @Override
    public String toString() {
        return "DispatcherDTO [process=" + process + ", limit=" + limit + ", additionalReference=" + additionalReference
                + "]";
    }
}
