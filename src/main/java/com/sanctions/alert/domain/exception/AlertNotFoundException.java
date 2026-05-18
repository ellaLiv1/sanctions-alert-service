package com.sanctions.alert.domain.exception;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(String id) {
        super("Alert not found: " + id);
    }
}
