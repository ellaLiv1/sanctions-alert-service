package com.sanctions.alert.dto;

import com.sanctions.alert.domain.AlertStatus;

import java.time.Instant;

public class EscalateAlertResponse {

    private final AlertStatus status;
    private final Instant updatedAt;

    public EscalateAlertResponse(AlertStatus status, Instant updatedAt) {
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public AlertStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
}
