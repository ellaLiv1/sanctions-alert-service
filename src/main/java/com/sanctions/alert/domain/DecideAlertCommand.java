package com.sanctions.alert.domain;

/**
 * Internal command object representing the intent to decide on an alert.
 * Carries data from the controller layer to the service layer.
 */
public class DecideAlertCommand {

    private final String tenantId;
    private final String alertId;
    private final AlertStatus status;
    private final String decisionNote;

    public DecideAlertCommand(String tenantId, String alertId, AlertStatus status, String decisionNote) {
        this.tenantId = tenantId;
        this.alertId = alertId;
        this.status = status;
        this.decisionNote = decisionNote;
    }

    public String getTenantId() { return tenantId; }
    public String getAlertId() { return alertId; }
    public AlertStatus getStatus() { return status; }
    public String getDecisionNote() { return decisionNote; }
}
