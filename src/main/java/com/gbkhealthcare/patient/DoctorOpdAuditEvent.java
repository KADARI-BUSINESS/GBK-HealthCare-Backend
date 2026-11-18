package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_opd_audit_events", indexes = {
        @Index(name = "idx_doctor_opd_audit_encounter_time", columnList = "patient_visit_id, occurred_at"),
        @Index(name = "idx_doctor_opd_audit_actor_time", columnList = "doctor_id, occurred_at")})
public class DoctorOpdAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "patient_visit_id", nullable = false) private Long patientVisitId;
    @Column(name = "queue_entry_id", nullable = false) private Long queueEntryId;
    @Column(name = "action_type", nullable = false, length = 32) private String actionType;
    @Column(name = "occurred_at", nullable = false, updatable = false) private LocalDateTime occurredAt;
    protected DoctorOpdAuditEvent() {}
    public DoctorOpdAuditEvent(Long doctorId, Long visitId, Long queueId, String action, LocalDateTime at) {
        this.doctorId = doctorId; patientVisitId = visitId; queueEntryId = queueId; actionType = action; occurredAt = at;
    }
    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public Long getPatientVisitId() { return patientVisitId; }
    public Long getQueueEntryId() { return queueEntryId; }
    public String getActionType() { return actionType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
