package com.sanctions.alert.domain;

import com.sanctions.alert.domain.exception.AlreadyDecidedException;
import com.sanctions.alert.domain.exception.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Alert domain aggregate")
class AlertTest {

    private Alert openAlert;
    private static final Instant NOW = Instant.parse("2024-01-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        openAlert = new Alert("id-1", "tenant-A", "tx-99", "OFAC Corp", 85, null, NOW);
    }

    // ── Escalation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("escalate()")
    class Escalate {

        @Test
        @DisplayName("transitions OPEN → ESCALATED and updates updatedAt")
        void escalatesFromOpen() {
            Instant later = NOW.plusSeconds(60);
            openAlert.escalate(later);

            assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.ESCALATED);
            assertThat(openAlert.getUpdatedAt()).isEqualTo(later);
        }

        @Test
        @DisplayName("throws InvalidTransitionException when already ESCALATED")
        void cannotEscalateFromEscalated() {
            openAlert.escalate(NOW);
            assertThatThrownBy(() -> openAlert.escalate(NOW))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("throws InvalidTransitionException when status is CLEARED")
        void cannotEscalateFromCleared() {
            openAlert.decide(AlertStatus.CLEARED, "no match", NOW);
            assertThatThrownBy(() -> openAlert.escalate(NOW))
                    .isInstanceOf(InvalidTransitionException.class);
        }

        @Test
        @DisplayName("throws InvalidTransitionException when status is CONFIRMED_HIT")
        void cannotEscalateFromConfirmedHit() {
            openAlert.decide(AlertStatus.CONFIRMED_HIT, "real match", NOW);
            assertThatThrownBy(() -> openAlert.escalate(NOW))
                    .isInstanceOf(InvalidTransitionException.class);
        }
    }

    // ── Decision ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("decide()")
    class Decide {

        @Test
        @DisplayName("transitions OPEN → CLEARED and stores decisionNote")
        void clearsFromOpen() {
            openAlert.decide(AlertStatus.CLEARED, "no real match", NOW);

            assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.CLEARED);
            assertThat(openAlert.getDecisionNote()).isEqualTo("no real match");
        }

        @Test
        @DisplayName("transitions OPEN → CONFIRMED_HIT")
        void confirmsHitFromOpen() {
            openAlert.decide(AlertStatus.CONFIRMED_HIT, "exact match", NOW);
            assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.CONFIRMED_HIT);
        }

        @Test
        @DisplayName("transitions ESCALATED → CLEARED")
        void clearsFromEscalated() {
            openAlert.escalate(NOW);
            openAlert.decide(AlertStatus.CLEARED, "resolved", NOW);
            assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.CLEARED);
        }

        @Test
        @DisplayName("transitions ESCALATED → CONFIRMED_HIT")
        void confirmsHitFromEscalated() {
            openAlert.escalate(NOW);
            openAlert.decide(AlertStatus.CONFIRMED_HIT, "confirmed", NOW);
            assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.CONFIRMED_HIT);
        }

        @Test
        @DisplayName("write-once: throws AlreadyDecidedException on second decision")
        void writeOnce() {
            openAlert.decide(AlertStatus.CLEARED, "first", NOW);
            assertThatThrownBy(() -> openAlert.decide(AlertStatus.CONFIRMED_HIT, "second", NOW))
                    .isInstanceOf(AlreadyDecidedException.class);
        }

        @Test
        @DisplayName("throws InvalidTransitionException for non-decision status like ESCALATED")
        void rejectsNonDecisionStatus() {
            assertThatThrownBy(() -> openAlert.decide(AlertStatus.ESCALATED, "note", NOW))
                    .isInstanceOf(InvalidTransitionException.class);
        }
    }

    // ── Initial state ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("new alert starts with OPEN status")
    void initialStatusIsOpen() {
        assertThat(openAlert.getStatus()).isEqualTo(AlertStatus.OPEN);
    }

    @Test
    @DisplayName("new alert has no decisionNote")
    void initialDecisionNoteIsNull() {
        assertThat(openAlert.getDecisionNote()).isNull();
    }
}
