package com.sanctions.alert.domain.exception;

import com.sanctions.alert.domain.AlertStatus;

public class InvalidTransitionException extends RuntimeException {
    public InvalidTransitionException(AlertStatus from, AlertStatus to) {
        super("Cannot transition from " + from + " to " + to + ".");
    }
}
