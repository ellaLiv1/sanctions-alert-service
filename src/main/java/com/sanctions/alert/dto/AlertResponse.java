package com.sanctions.alert.dto;

import com.sanctions.alert.domain.AlertStatus;

import java.time.Instant;

public class AlertResponse {

    private final String id;
    private final String transactionId;
    private final String matchedEntityName;
    private final int matchScore;
    private final AlertStatus status;
    private final String assignedTo;
    private final String decisionNote;
    private final Instant createdAt;
    private final Instant updatedAt;

    public AlertResponse(String id, String transactionId, String matchedEntityName,
                         int matchScore, AlertStatus status, String assignedTo, String decisionNote,
                         Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.matchedEntityName = matchedEntityName;
        this.matchScore = matchScore;
        this.status = status;
        this.assignedTo = assignedTo;
        this.decisionNote = decisionNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public String getMatchedEntityName() { return matchedEntityName; }
    public int getMatchScore() { return matchScore; }
    public AlertStatus getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
