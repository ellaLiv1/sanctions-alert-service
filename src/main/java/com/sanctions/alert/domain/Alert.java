package com.sanctions.alert.domain;

import com.sanctions.alert.domain.exception.AlreadyDecidedException;
import com.sanctions.alert.domain.exception.InvalidTransitionException;

import java.time.Instant;

/**
 * Alert aggregate root.
 *
 * All state transitions are encapsulated here. The domain layer owns the rules;
 * no status mutations happen outside this class.
 */
public class Alert {

    private final String id;
    private final String tenantId;
    private final String transactionId;
    private final String matchedEntityName;
    private final int matchScore;
    private AlertStatus status;
    private String assignedTo;
    private String decisionNote;
    private final Instant createdAt;
    private Instant updatedAt;

    /** Called by the service when creating a new alert. */
    public Alert(String id,
                 String tenantId,
                 String transactionId,
                 String matchedEntityName,
                 int matchScore,
                 String assignedTo,
                 Instant now) {
        this.id = id;
        this.tenantId = tenantId;
        this.transactionId = transactionId;
        this.matchedEntityName = matchedEntityName;
        this.matchScore = matchScore;
        this.assignedTo = assignedTo;
        this.status = AlertStatus.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Transitions the alert to ESCALATED.
     * Only valid when current status is OPEN.
     *
     * @throws AlreadyDecidedException  if the alert has a terminal status
     * @throws InvalidTransitionException if the current status does not allow escalation
     */
    public void escalate(Instant now) {
        if (status != AlertStatus.OPEN) {
            throw new InvalidTransitionException(status, AlertStatus.ESCALATED);
        }
        this.status = AlertStatus.ESCALATED;
        this.updatedAt = now;
    }

    /**
     * Records a terminal compliance decision (CLEARED or CONFIRMED_HIT).
     * Write-once: a decided alert cannot be re-decided.
     *
     * @throws AlreadyDecidedException   if the alert already has a terminal status
     * @throws InvalidTransitionException if the supplied target status is not a decision status
     */
    public void decide(AlertStatus decision, String note, Instant now) {
        if (status.isTerminal()) {
            throw new AlreadyDecidedException(id);
        }
        if (!decision.isDecision()) {
            throw new InvalidTransitionException(status, decision);
        }
        this.status = decision;
        this.decisionNote = note;
        this.updatedAt = now;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getId()                { return id; }
    public String getTenantId()          { return tenantId; }
    public String getTransactionId()     { return transactionId; }
    public String getMatchedEntityName() { return matchedEntityName; }
    public int getMatchScore()           { return matchScore; }
    public AlertStatus getStatus()       { return status; }
    public String getAssignedTo()        { return assignedTo; }
    public String getDecisionNote()      { return decisionNote; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }
}
