package com.sanctions.alert.service;

import com.sanctions.alert.domain.Alert;
import com.sanctions.alert.domain.AlertStatus;
import com.sanctions.alert.domain.CreateAlertCommand;
import com.sanctions.alert.domain.DecideAlertCommand;
import com.sanctions.alert.domain.EscalateAlertCommand;
import com.sanctions.alert.domain.exception.AlertNotFoundException;
import com.sanctions.alert.events.DecidedAlertEvent;
import com.sanctions.alert.events.EscalatedAlertEvent;
import com.sanctions.alert.events.EventPublisher;
import com.sanctions.alert.repository.AlertFilter;
import com.sanctions.alert.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository repository;
    private final EventPublisher eventPublisher;

    public AlertService(AlertRepository repository, EventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public Alert createAlert(CreateAlertCommand cmd) {
        Instant now = Instant.now();
        Alert alert = new Alert(
                UUID.randomUUID().toString(),
                cmd.getTenantId(),
                cmd.getTransactionId(),
                cmd.getMatchedEntityName(),
                cmd.getMatchScore(),
                cmd.getAssignedTo(),
                now
        );
        return repository.save(alert);
    }

    public List<Alert> listAlerts(String tenantId, AlertStatus status, Integer minMatchScore) {
        return repository.findAll(new AlertFilter(tenantId, status, minMatchScore));
    }

    public Alert escalateAlert(EscalateAlertCommand cmd) {
        Alert alert = findOrThrow(cmd.getTenantId(), cmd.getAlertId());
        alert.escalate(Instant.now());
        Alert updated = repository.update(alert);
        eventPublisher.publish(new EscalatedAlertEvent(alert.getId(), alert.getTenantId(), alert.getUpdatedAt()));
        return updated;
    }

    public Alert decideAlert(DecideAlertCommand cmd) {
        Alert alert = findOrThrow(cmd.getTenantId(), cmd.getAlertId());
        alert.decide(cmd.getStatus(), cmd.getDecisionNote(), Instant.now());
        Alert updated = repository.update(alert);
        eventPublisher.publish(new DecidedAlertEvent(alert.getId(), alert.getTenantId(), alert.getStatus().name(), alert.getUpdatedAt()));
        return updated;
    }

    private Alert findOrThrow(String tenantId, String alertId) {
        return repository.findByIdAndTenantId(alertId, tenantId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }
}
