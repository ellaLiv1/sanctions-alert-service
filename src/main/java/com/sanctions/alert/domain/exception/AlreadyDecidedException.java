package com.sanctions.alert.domain.exception;

public class AlreadyDecidedException extends RuntimeException {
    public AlreadyDecidedException(String alertId) {
        super("Alert '" + alertId + "' has already been decided and cannot be transitioned further.");
    }
}
