package com.sanctions.alert.repository;

import com.sanctions.alert.domain.Alert;
import com.sanctions.alert.domain.AlertStatus;

import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for Alerts.
 *
 * All methods are tenant-scoped. Implementations MUST NOT return data
 * belonging to a different tenant, even if the id matches.
 *
 * Swap this interface's implementation to move from in-memory → JPA → DynamoDB
 * without touching the service layer.
 */
public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findByIdAndTenantId(String id, String tenantId);

    List<Alert> findAll(AlertFilter filter);

    Alert update(Alert alert);
}
