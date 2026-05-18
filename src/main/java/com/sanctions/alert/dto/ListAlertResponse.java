package com.sanctions.alert.dto;

import java.util.List;

public class ListAlertResponse {

    private final List<AlertResponse> alerts;

    public ListAlertResponse(List<AlertResponse> alerts) {
        this.alerts = alerts;
    }

    public List<AlertResponse> getAlerts() { return alerts; }
}
