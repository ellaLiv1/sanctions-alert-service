package com.sanctions.alert.domain;

/**
 * Internal command object representing the intent to create a new alert.
 * Carries data from the controller layer to the service layer.
 */
public class CreateAlertCommand {

    private final String tenantId;
    private final String transactionId;
    private final String matchedEntityName;
    private final int matchScore;
    private final String assignedTo;

    public CreateAlertCommand(String tenantId, String transactionId, String matchedEntityName,
                               int matchScore, String assignedTo) {
        this.tenantId = tenantId;
        this.transactionId = transactionId;
        this.matchedEntityName = matchedEntityName;
        this.matchScore = matchScore;
        this.assignedTo = assignedTo;
    }

    public String getTenantId() { return tenantId; }
    public String getTransactionId() { return transactionId; }
    public String getMatchedEntityName() { return matchedEntityName; }
    public int getMatchScore() { return matchScore; }
    public String getAssignedTo() { return assignedTo; }
}
