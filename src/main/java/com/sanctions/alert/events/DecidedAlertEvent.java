package com.sanctions.alert.events;

import java.time.Instant;

/**
 * Domain event published when an alert decision is submitted.
 * The decision is stored as String (not AlertStatus enum) because events are
 * self-contained messages meant for external consumers who don't know about
 * internal domain enums.
 */
public final class DecidedAlertEvent implements AlertEvent {

    private final String alertId;
    private final String tenantId;
    private final String decision;
    private final Instant timestamp;

    public DecidedAlertEvent(String alertId, String tenantId, String decision, Instant timestamp) {
        this.alertId = alertId;
        this.tenantId = tenantId;
        this.decision = decision;
        this.timestamp = timestamp;
    }

    @Override
    public String alertId() { return alertId; }

    @Override
    public String tenantId() { return tenantId; }

    @Override
    public Instant timestamp() { return timestamp; }

    public String getDecision() { return decision; }
}
