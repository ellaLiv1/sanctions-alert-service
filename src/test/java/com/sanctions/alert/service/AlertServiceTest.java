package com.sanctions.alert.service;

import com.sanctions.alert.domain.Alert;
import com.sanctions.alert.domain.AlertStatus;
import com.sanctions.alert.domain.CreateAlertCommand;
import com.sanctions.alert.domain.DecideAlertCommand;
import com.sanctions.alert.domain.EscalateAlertCommand;
import com.sanctions.alert.domain.exception.AlertNotFoundException;
import com.sanctions.alert.domain.exception.AlreadyDecidedException;
import com.sanctions.alert.domain.exception.InvalidTransitionException;
import com.sanctions.alert.events.AlertEvent;
import com.sanctions.alert.events.DecidedAlertEvent;
import com.sanctions.alert.events.EscalatedAlertEvent;
import com.sanctions.alert.events.EventPublisher;
import com.sanctions.alert.repository.AlertFilter;
import com.sanctions.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService")
class AlertServiceTest {

    @Mock AlertRepository repository;
    @Mock EventPublisher eventPublisher;

    AlertService service;

    private static final String TENANT = "tenant-X";
    private static final String ALERT_ID = "alert-1";
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        service = new AlertService(repository, eventPublisher);
    }

    @Test
    @DisplayName("createAlert saves with OPEN status and returns persisted alert")
    void createAlert_savesAndReturns() {
        CreateAlertCommand cmd = new CreateAlertCommand(TENANT, "tx-1", "OFAC Corp", 90, null);
        Alert saved = buildAlert(AlertStatus.OPEN);
        when(repository.save(any())).thenReturn(saved);
        Alert result = service.createAlert(cmd);
        assertThat(result.getStatus()).isEqualTo(AlertStatus.OPEN);
        verify(repository).save(any(Alert.class));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("listAlerts passes correct filter to repository")
    void listAlerts_passesFilter() {
        when(repository.findAll(any())).thenReturn(List.of());
        service.listAlerts(TENANT, AlertStatus.OPEN, 50);
        ArgumentCaptor<AlertFilter> cap = ArgumentCaptor.forClass(AlertFilter.class);
        verify(repository).findAll(cap.capture());
        AlertFilter f = cap.getValue();
        assertThat(f.getTenantId()).isEqualTo(TENANT);
        assertThat(f.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(f.getMinMatchScore()).isEqualTo(50);
    }

    @Test
    @DisplayName("escalateAlert transitions to ESCALATED and publishes event")
    void escalateAlert_publishesEvent() {
        Alert alert = buildAlert(AlertStatus.OPEN);
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.of(alert));
        when(repository.update(alert)).thenReturn(alert);
        service.escalateAlert(new EscalateAlertCommand(TENANT, ALERT_ID));
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ESCALATED);
        ArgumentCaptor<AlertEvent> cap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventPublisher).publish(cap.capture());
        assertThat(cap.getValue()).isInstanceOf(EscalatedAlertEvent.class);
    }

    @Test
    @DisplayName("escalateAlert throws AlertNotFoundException when alert not found for tenant")
    void escalateAlert_notFound() {
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.escalateAlert(new EscalateAlertCommand(TENANT, ALERT_ID)))
                .isInstanceOf(AlertNotFoundException.class);
    }

    @Test
    @DisplayName("escalateAlert throws InvalidTransitionException for terminal alert")
    void escalateAlert_alreadyDecided() {
        Alert alert = buildAlert(AlertStatus.CLEARED);
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.of(alert));
        assertThatThrownBy(() -> service.escalateAlert(new EscalateAlertCommand(TENANT, ALERT_ID)))
                .isInstanceOf(InvalidTransitionException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("decideAlert with CLEARED transitions status and publishes Decided event")
    void decideAlert_cleared() {
        Alert alert = buildAlert(AlertStatus.OPEN);
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.of(alert));
        when(repository.update(alert)).thenReturn(alert);
        service.decideAlert(new DecideAlertCommand(TENANT, ALERT_ID, AlertStatus.CLEARED, "no match found"));
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.CLEARED);
        assertThat(alert.getDecisionNote()).isEqualTo("no match found");
        ArgumentCaptor<AlertEvent> cap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventPublisher).publish(cap.capture());
        DecidedAlertEvent event = (DecidedAlertEvent) cap.getValue();
        assertThat(event.getDecision()).isEqualTo(AlertStatus.CLEARED.name());
    }

    @Test
    @DisplayName("decideAlert throws AlreadyDecidedException on second decision (write-once)")
    void decideAlert_writeOnce() {
        Alert alert = buildAlert(AlertStatus.CONFIRMED_HIT);
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.of(alert));
        assertThatThrownBy(() -> service.decideAlert(new DecideAlertCommand(TENANT, ALERT_ID, AlertStatus.CLEARED, "note")))
                .isInstanceOf(AlreadyDecidedException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("decideAlert throws InvalidTransitionException for non-decision status")
    void decideAlert_invalidStatus() {
        Alert alert = buildAlert(AlertStatus.OPEN);
        when(repository.findByIdAndTenantId(ALERT_ID, TENANT)).thenReturn(Optional.of(alert));
        assertThatThrownBy(() -> service.decideAlert(new DecideAlertCommand(TENANT, ALERT_ID, AlertStatus.ESCALATED, "note")))
                .isInstanceOf(InvalidTransitionException.class);
    }

    private Alert buildAlert(AlertStatus status) {
        Alert a = new Alert(ALERT_ID, TENANT, "tx-1", "OFAC Corp", 80, null, NOW);
        if (status == AlertStatus.ESCALATED) a.escalate(NOW);
        if (status == AlertStatus.CLEARED) a.decide(AlertStatus.CLEARED, "note", NOW);
        if (status == AlertStatus.CONFIRMED_HIT) a.decide(AlertStatus.CONFIRMED_HIT, "note", NOW);
        return a;
    }
}
