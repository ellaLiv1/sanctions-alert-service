package com.sanctions.alert.mapper;

import com.sanctions.alert.domain.Alert;
import com.sanctions.alert.dto.AlertResponse;
import com.sanctions.alert.dto.DecideAlertResponse;
import com.sanctions.alert.dto.EscalateAlertResponse;

public class AlertMapper {

    private AlertMapper() {}

    public static AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getTransactionId(),
                alert.getMatchedEntityName(),
                alert.getMatchScore(),
                alert.getStatus(),
                alert.getAssignedTo(),
                alert.getDecisionNote(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }

    public static EscalateAlertResponse toEscalateResponse(Alert alert) {
        return new EscalateAlertResponse(
                alert.getStatus(),
                alert.getUpdatedAt()
        );
    }

    public static DecideAlertResponse toDecideResponse(Alert alert) {
        return new DecideAlertResponse(
                alert.getStatus(),
                alert.getDecisionNote(),
                alert.getUpdatedAt()
        );
    }
}
