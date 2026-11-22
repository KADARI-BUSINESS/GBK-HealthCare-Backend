package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_history_access_events", indexes = {
        @Index(name = "idx_history_access_doctor_time", columnList = "doctor_id, accessed_at"),
        @Index(name = "idx_history_access_patient_time", columnList = "patient_id, accessed_at"),
        @Index(name = "idx_history_access_encounter", columnList = "encounter_id, accessed_at")})
public class PatientHistoryAccessEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "encounter_id", nullable = false) private Long encounterId;
    @Column(name = "accessed_resource", nullable = false, length = 48) private String accessedResource;
    @Column(name = "accessed_resource_id") private Long accessedResourceId;
    @Column(name = "accessed_at", nullable = false, updatable = false) private LocalDateTime accessedAt;
    protected PatientHistoryAccessEvent() {}
    public PatientHistoryAccessEvent(Long doctorId, Long patientId, Long encounterId, String resource, Long resourceId, LocalDateTime at) {
        this.doctorId = doctorId; this.patientId = patientId; this.encounterId = encounterId;
        this.accessedResource = resource; this.accessedResourceId = resourceId; this.accessedAt = at;
    }
    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public Long getPatientId() { return patientId; }
    public Long getEncounterId() { return encounterId; }
    public String getAccessedResource() { return accessedResource; }
    public Long getAccessedResourceId() { return accessedResourceId; }
    public LocalDateTime getAccessedAt() { return accessedAt; }
}
