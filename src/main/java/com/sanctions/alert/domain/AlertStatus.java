package com.sanctions.alert.domain;

/**
 * Lifecycle states for an Alert.
 *
 * Valid transitions:
 *   OPEN → ESCALATED  (via escalate)
 *   OPEN → CLEARED    (via decide)
 *   OPEN → CONFIRMED_HIT (via decide)
 *   ESCALATED → CLEARED (via decide)
 *   ESCALATED → CONFIRMED_HIT (via decide)
 *
 * CLEARED and CONFIRMED_HIT are terminal: no further transitions allowed.
 */
public enum AlertStatus {
    OPEN,
    ESCALATED,
    CLEARED,
    CONFIRMED_HIT;

    public boolean isTerminal() {
        return this == CLEARED || this == CONFIRMED_HIT;
    }

    public boolean isDecision() {
        return this == CLEARED || this == CONFIRMED_HIT;
    }

    public boolean canDecide() {
        return this == OPEN || this == ESCALATED;
    }
}
