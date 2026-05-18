package com.sanctions.alert.dto;

import com.sanctions.alert.domain.AlertStatus;

import java.time.Instant;

public class DecideAlertResponse {

    private final AlertStatus status;
    private final String decisionNote;
    private final Instant updatedAt;

    public DecideAlertResponse(AlertStatus status, String decisionNote, Instant updatedAt) {
        this.status = status;
        this.decisionNote = decisionNote;
        this.updatedAt = updatedAt;
    }

    public AlertStatus getStatus() { return status; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getUpdatedAt() { return updatedAt; }
}
