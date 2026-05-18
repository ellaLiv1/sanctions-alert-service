package com.sanctions.alert.controller;

import com.sanctions.alert.domain.AlertStatus;
import com.sanctions.alert.domain.CreateAlertCommand;
import com.sanctions.alert.domain.DecideAlertCommand;
import com.sanctions.alert.domain.EscalateAlertCommand;
import com.sanctions.alert.dto.AlertResponse;
import com.sanctions.alert.dto.CreateAlertRequest;
import com.sanctions.alert.dto.DecideAlertRequest;
import com.sanctions.alert.dto.DecideAlertResponse;
import com.sanctions.alert.dto.EscalateAlertResponse;
import com.sanctions.alert.dto.ListAlertResponse;
import com.sanctions.alert.mapper.AlertMapper;
import com.sanctions.alert.middleware.TenantContext;
import com.sanctions.alert.service.AlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(@Valid @RequestBody CreateAlertRequest req) {
        String tenantId = TenantContext.get();
        CreateAlertCommand cmd = new CreateAlertCommand(
                tenantId,
                req.getTransactionId(),
                req.getMatchedEntityName(),
                req.getMatchScore(),
                req.getAssignedTo()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AlertMapper.toResponse(alertService.createAlert(cmd)));
    }

    @GetMapping
    public ResponseEntity<ListAlertResponse> listAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) Integer minMatchScore
    ) {
        String tenantId = TenantContext.get();
        ListAlertResponse response = new ListAlertResponse(
                alertService.listAlerts(tenantId, status, minMatchScore)
                        .stream()
                        .map(AlertMapper::toResponse)
                        .collect(Collectors.toList())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<EscalateAlertResponse> escalateAlert(@PathVariable String id) {
        String tenantId = TenantContext.get();
        EscalateAlertCommand cmd = new EscalateAlertCommand(tenantId, id);
        return ResponseEntity.ok(AlertMapper.toEscalateResponse(alertService.escalateAlert(cmd)));
    }

    @PostMapping("/{id}/decide")
    public ResponseEntity<DecideAlertResponse> decideAlert(
            @PathVariable String id,
            @Valid @RequestBody DecideAlertRequest req
    ) {
        String tenantId = TenantContext.get();
        DecideAlertCommand cmd = new DecideAlertCommand(
                tenantId,
                id,
                req.getDecision(),
                req.getDecisionNote()
        );
        return ResponseEntity.ok(AlertMapper.toDecideResponse(alertService.decideAlert(cmd)));
    }
}
