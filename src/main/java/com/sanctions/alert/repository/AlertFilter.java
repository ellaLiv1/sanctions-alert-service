package com.sanctions.alert.repository;

import com.sanctions.alert.domain.AlertStatus;

/**
 * Filter criteria for listing alerts.
 * tenantId is always required — enforced by the service before reaching the repository.
 */
public class AlertFilter {

    private final String tenantId;
    private final AlertStatus status;
    private final Integer minMatchScore;

    public AlertFilter(String tenantId, AlertStatus status, Integer minMatchScore) {
        this.tenantId = tenantId;
        this.status = status;
        this.minMatchScore = minMatchScore;
    }

    public String getTenantId() { return tenantId; }
    public AlertStatus getStatus() { return status; }
    public Integer getMinMatchScore() { return minMatchScore; }
}
