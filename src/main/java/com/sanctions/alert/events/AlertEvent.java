package com.sanctions.alert.events;

import java.time.Instant;

/**
 * Interface for domain events.
 * Open for extension — new event types can be added without modifying existing code.
 * Not sealed because this is an internal contract owned entirely by this service.
 * New event types will always be added by developers who will update the publisher accordingly.
 */
public interface AlertEvent {
    String alertId();
    String tenantId();
    Instant timestamp();
}
