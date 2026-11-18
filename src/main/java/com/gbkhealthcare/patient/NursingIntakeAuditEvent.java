package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nursing_intake_audit_events", indexes = {
        @Index(name = "idx_nursing_audit_queue_time", columnList = "queue_entry_id, occurred_at, id"),
        @Index(name = "idx_nursing_audit_actor_time", columnList = "actor_staff_id, occurred_at, id")})
public class NursingIntakeAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "queue_entry_id", nullable = false) private Long queueEntryId;
    @Column(name = "intake_record_id") private Long intakeRecordId;
    @Column(name = "actor_staff_id", nullable = false) private Long actorStaffId;
    @Column(name = "actor_role", nullable = false, length = 16) private String actorRole = "NURSE";
    @Column(name = "action_type", nullable = false, length = 24) private String actionType;
    @Column(nullable = false) private Integer revision;
    @Column(name = "occurred_at", nullable = false, updatable = false) private LocalDateTime occurredAt;
    protected NursingIntakeAuditEvent() {}
    public NursingIntakeAuditEvent(Long queueId, Long recordId, Long staffId, String action, Integer revision, LocalDateTime at) {
        queueEntryId = queueId; intakeRecordId = recordId; actorStaffId = staffId; actionType = action; this.revision = revision; occurredAt = at;
    }
    public Long getId() { return id; }
    public Long getQueueEntryId() { return queueEntryId; }
    public Long getIntakeRecordId() { return intakeRecordId; }
    public Long getActorStaffId() { return actorStaffId; }
    public String getActorRole() { return actorRole; }
    public String getActionType() { return actionType; }
    public Integer getRevision() { return revision; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
