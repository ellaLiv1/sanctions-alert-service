package com.sanctions.alert.domain;

/**
 * Internal command object representing the intent to escalate an alert.
 * Carries data from the controller layer to the service layer.
 */
public class EscalateAlertCommand {

    private final String tenantId;
    private final String alertId;

    public EscalateAlertCommand(String tenantId, String alertId) {
        this.tenantId = tenantId;
        this.alertId = alertId;
    }

    public String getTenantId() { return tenantId; }
    public String getAlertId() { return alertId; }
}
