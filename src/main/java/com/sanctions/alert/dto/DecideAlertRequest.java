package com.sanctions.alert.dto;

import com.sanctions.alert.domain.AlertStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DecideAlertRequest {

    @NotNull(message = "decision is required (CLEARED or CONFIRMED_HIT)")
    private final AlertStatus decision;

    @NotBlank(message = "decisionNote is required when submitting a decision")
    private final String decisionNote;

    public DecideAlertRequest(AlertStatus decision, String decisionNote) {
        this.decision = decision;
        this.decisionNote = decisionNote;
    }

    public AlertStatus getDecision() { return decision; }
    public String getDecisionNote() { return decisionNote; }
}
