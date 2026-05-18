package com.sanctions.alert.events;

import java.time.Instant;

/**
 * Domain event published when an alert is escalated.
 */
public final class EscalatedAlertEvent implements AlertEvent {

    private final String alertId;
    private final String tenantId;
    private final Instant timestamp;

    public EscalatedAlertEvent(String alertId, String tenantId, Instant timestamp) {
        this.alertId = alertId;
        this.tenantId = tenantId;
        this.timestamp = timestamp;
    }

    @Override
    public String alertId() { return alertId; }

    @Override
    public String tenantId() { return tenantId; }

    @Override
    public Instant timestamp() { return timestamp; }
}
