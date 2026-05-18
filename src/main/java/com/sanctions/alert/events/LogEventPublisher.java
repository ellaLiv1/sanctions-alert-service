package com.sanctions.alert.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class LogEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger("events");
    private final ObjectMapper mapper;

    public LogEventPublisher(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void publish(AlertEvent event) {
        try {
            Object payload = toPayload(event);
            log.info("DOMAIN_EVENT {}", mapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to serialize domain event for alertId={}", event.alertId(), e);
        }
    }

    private Object toPayload(AlertEvent event) {
        return switch (event) {
            case EscalatedAlertEvent e -> new EscalatedPayload(
                    "alert.escalated", e.alertId(), e.tenantId(), "ESCALATED", e.timestamp()
            );
            case DecidedAlertEvent d -> new DecidedPayload(
                    "alert.decided", d.alertId(), d.tenantId(), d.getDecision(), d.timestamp()
            );
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass().getName()
            );
        };
    }

    private static class EscalatedPayload {
        public final String event;
        public final String alertId;
        public final String tenantId;
        public final String outcome;
        public final Instant timestamp;

        public EscalatedPayload(String event, String alertId, String tenantId, String outcome, Instant timestamp) {
            this.event = event;
            this.alertId = alertId;
            this.tenantId = tenantId;
            this.outcome = outcome;
            this.timestamp = timestamp;
        }
    }

    private static class DecidedPayload {
        public final String event;
        public final String alertId;
        public final String tenantId;
        public final String decision;
        public final Instant timestamp;

        public DecidedPayload(String event, String alertId, String tenantId, String decision, Instant timestamp) {
            this.event = event;
            this.alertId = alertId;
            this.tenantId = tenantId;
            this.decision = decision;
            this.timestamp = timestamp;
        }
    }
}
