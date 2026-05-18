package com.sanctions.alert.repository;

import com.sanctions.alert.domain.Alert;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryAlertRepository implements AlertRepository {

    private final Map<String, Alert> store = new ConcurrentHashMap<>();

    @Override
    public Alert save(Alert alert) {
        store.put(alert.getId(), alert);
        return alert;
    }

    @Override
    public Optional<Alert> findByIdAndTenantId(String id, String tenantId) {
        return Optional.ofNullable(store.get(id))
                .filter(a -> a.getTenantId().equals(tenantId));
    }

    @Override
    public List<Alert> findAll(AlertFilter filter) {
        return store.values().stream()
                .filter(a -> a.getTenantId().equals(filter.getTenantId()))
                .filter(a -> filter.getStatus() == null || a.getStatus() == filter.getStatus())
                .filter(a -> filter.getMinMatchScore() == null || a.getMatchScore() >= filter.getMinMatchScore())
                .collect(Collectors.toList());
    }

    @Override
    public Alert update(Alert alert) {
        store.put(alert.getId(), alert);
        return alert;
    }
}
