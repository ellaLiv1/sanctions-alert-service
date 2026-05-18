package com.sanctions.alert.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateAlertRequest {

    @NotBlank(message = "transactionId is required")
    private final String transactionId;

    @NotBlank(message = "matchedEntityName is required")
    private final String matchedEntityName;

    @Min(value = 0, message = "matchScore must be between 0 and 100")
    @Max(value = 100, message = "matchScore must be between 0 and 100")
    private final int matchScore;

    private final String assignedTo;

    public CreateAlertRequest(String transactionId, String matchedEntityName, int matchScore, String assignedTo) {
        this.transactionId = transactionId;
        this.matchedEntityName = matchedEntityName;
        this.matchScore = matchScore;
        this.assignedTo = assignedTo;
    }

    public String getTransactionId() { return transactionId; }
    public String getMatchedEntityName() { return matchedEntityName; }
    public int getMatchScore() { return matchScore; }
    public String getAssignedTo() { return assignedTo; }
}
