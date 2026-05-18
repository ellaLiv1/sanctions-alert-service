package com.sanctions.alert.events;

/**
 * Port for publishing domain events.
 *
 * The service layer depends only on this interface.
 * Swap the implementation for Kafka/NATS/SQS without touching business logic.
 */
public interface EventPublisher {
    void publish(AlertEvent event);
}
