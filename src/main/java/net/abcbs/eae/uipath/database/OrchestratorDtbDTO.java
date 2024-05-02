package net.abcbs.eae.uipath.database;


public class OrchestratorDtbDTO {
    
    /* * * * * * * * * * * * * *
     * Following are the UiPath 
     * Orchestrator DataBase
     * private data points
     * * * * * * * * * * * * * */
    private int id;
    private int queueDefinitionId;
    private String reference;
    private String specificData;
    
    /* * * * * * * * * * * * * *
     * UiPath Orchestrator DataBase
     * private data points 
     * getters/setters
     * * * * * * * * * * * * * */
    public String getSpecificData() {
        return specificData;
    }
    public String getReference() {
        return reference;
    }
    public int getQueueDefinitionId() {
        return queueDefinitionId;
    }
    public int getId() {
        return id;
    }
    public void setSpecificData(String specificData) {
        this.specificData = specificData;
    }
    public void setReference(String itemReference) {
        this.reference = itemReference;
    }
    public void setQueueDefinitionId(int queueDefinitionId) {
        this.queueDefinitionId = queueDefinitionId;
    }
    public void setId(int itemId) {
        this.id = itemId;
    }

}
