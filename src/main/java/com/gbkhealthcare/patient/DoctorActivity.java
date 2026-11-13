package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_activity", indexes = {
        @Index(name = "idx_doctor_activity_doctor_time_id",
                columnList = "doctor_id, occurred_at, id")
})
public class DoctorActivity {

    public enum ActionType {
        PATIENT_REGISTERED,
        VISIT_RECORDED,
        FOLLOW_UP_SCHEDULED,
        FOLLOW_UP_RESCHEDULED,
        FOLLOW_UP_COMPLETED,
        FOLLOW_UP_CANCELLED
    }

    public enum EntityType {
        PATIENT,
        VISIT,
        FOLLOW_UP
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false, updatable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40, updatable = false)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20, updatable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private Long entityId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected DoctorActivity() {}

    DoctorActivity(Doctor doctor, ActionType actionType, EntityType entityType,
            Long entityId, LocalDateTime occurredAt) {
        this.doctor = doctor;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public ActionType getActionType() { return actionType; }
    public EntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
