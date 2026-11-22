package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_follow_up_audit_events", indexes = {
        @Index(name = "idx_follow_up_audit_follow_up_time", columnList = "follow_up_id, occurred_at"),
        @Index(name = "idx_follow_up_audit_actor_time", columnList = "actor_type, actor_id, occurred_at")
})
public class PatientFollowUpAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "follow_up_id", nullable = false, updatable = false) private Long followUpId;
    @Column(name = "previous_status", length = 20, updatable = false) private String previousStatus;
    @Column(name = "new_status", length = 20, nullable = false, updatable = false) private String newStatus;
    @Column(name = "action_type", length = 32, nullable = false, updatable = false) private String actionType;
    @Column(name = "actor_type", length = 16, nullable = false, updatable = false) private String actorType;
    @Column(name = "actor_id", nullable = false, updatable = false) private Long actorId;
    @Column(name = "reason", length = 1000, updatable = false) private String reason;
    @Column(name = "encounter_id", updatable = false) private Long encounterId;
    @Column(name = "occurred_at", nullable = false, updatable = false) private LocalDateTime occurredAt;

    protected PatientFollowUpAuditEvent() {}
    public PatientFollowUpAuditEvent(Long followUpId, String previousStatus, String newStatus,
            String actionType, String actorType, Long actorId, String reason, Long encounterId, LocalDateTime occurredAt) {
        this.followUpId = followUpId; this.previousStatus = previousStatus; this.newStatus = newStatus;
        this.actionType = actionType;
        this.actorType = actorType; this.actorId = actorId; this.reason = reason;
        this.encounterId = encounterId; this.occurredAt = occurredAt;
    }
    public Long getId() { return id; }
    public Long getFollowUpId() { return followUpId; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
    public String getActionType() { return actionType; }
    public String getActorType() { return actorType; }
    public Long getActorId() { return actorId; }
    public String getReason() { return reason; }
    public Long getEncounterId() { return encounterId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
