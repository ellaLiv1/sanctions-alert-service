package com.sanctions.alert.repository;

import com.sanctions.alert.domain.Alert;
import com.sanctions.alert.domain.AlertStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InMemoryAlertRepository")
class InMemoryAlertRepositoryTest {

    InMemoryAlertRepository repo;

    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        repo = new InMemoryAlertRepository();
    }

    @Test
    @DisplayName("save and findByIdAndTenantId returns the saved alert")
    void saveAndFind() {
        Alert a = alert("1", "tenant-A", 80);
        repo.save(a);
        Optional<Alert> found = repo.findByIdAndTenantId("1", "tenant-A");
        assertThat(found).isPresent().contains(a);
    }

    @Test
    @DisplayName("findByIdAndTenantId returns empty for wrong tenant")
    void tenantIsolation_find() {
        repo.save(alert("1", "tenant-A", 80));
        assertThat(repo.findByIdAndTenantId("1", "tenant-B")).isEmpty();
    }

    @Test
    @DisplayName("findAll filters by tenantId")
    void findAll_tenantFilter() {
        repo.save(alert("1", "tenant-A", 80));
        repo.save(alert("2", "tenant-B", 70));
        List<Alert> results = repo.findAll(new AlertFilter("tenant-A", null, null));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("1");
    }

    @Test
    @DisplayName("findAll filters by status")
    void findAll_statusFilter() {
        Alert open = alert("1", "tenant-A", 80);
        Alert escalated = alert("2", "tenant-A", 70);
        escalated.escalate(NOW);
        repo.save(open);
        repo.save(escalated);
        List<Alert> results = repo.findAll(new AlertFilter("tenant-A", AlertStatus.OPEN, null));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("1");
    }

    @Test
    @DisplayName("findAll filters by minMatchScore")
    void findAll_scoreFilter() {
        repo.save(alert("1", "tenant-A", 90));
        repo.save(alert("2", "tenant-A", 50));
        List<Alert> results = repo.findAll(new AlertFilter("tenant-A", null, 80));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMatchScore()).isEqualTo(90);
    }

    private Alert alert(String id, String tenantId, int score) {
        return new Alert(id, tenantId, "tx-" + id, "Corp " + id, score, null, NOW);
    }
}
