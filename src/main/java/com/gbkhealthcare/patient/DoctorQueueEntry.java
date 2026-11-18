package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Per-doctor encounter queue destination; consumed by the next Doctor workflow phase. */
@Entity
@Table(name = "doctor_queue_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_queue_nursing_entry", columnNames = "nursing_queue_entry_id"),
        @UniqueConstraint(name = "uk_doctor_queue_visit", columnNames = "patient_visit_id")},
        indexes = @Index(name = "idx_doctor_queue_doctor_status_created", columnList = "assigned_doctor_id, status, created_at"))
public class DoctorQueueEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "nursing_queue_entry_id", nullable = false) private NursingQueueEntry nursingQueueEntry;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_visit_id", nullable = false) private PatientVisit patientVisit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_doctor_id", nullable = false) private Doctor assignedDoctor;
    @Column(nullable = false, length = 24) private String status = "WAITING";
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "claimed_at") private LocalDateTime claimedAt;
    @Column(name = "consultation_started_at") private LocalDateTime consultationStartedAt;
    @Column(name = "held_at") private LocalDateTime heldAt;
    @Column(name = "signed_at") private LocalDateTime signedAt;
    protected DoctorQueueEntry() {}
    public DoctorQueueEntry(NursingQueueEntry queue, PatientVisit visit, Doctor doctor, LocalDateTime at) {
        nursingQueueEntry = queue; patientVisit = visit; assignedDoctor = doctor; createdAt = at;
    }
    public Long getId() { return id; }
    public NursingQueueEntry getNursingQueueEntry() { return nursingQueueEntry; }
    public PatientVisit getPatientVisit() { return patientVisit; }
    public Doctor getAssignedDoctor() { return assignedDoctor; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getConsultationStartedAt() { return consultationStartedAt; }
    public LocalDateTime getHeldAt() { return heldAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
    public void claim(LocalDateTime at) { require("WAITING"); status = "CLAIMED"; claimedAt = at; }
    public void start(LocalDateTime at) { require("CLAIMED"); status = "IN_CONSULTATION"; consultationStartedAt = at; }
    public void hold(LocalDateTime at) { require("IN_CONSULTATION"); status = "ON_HOLD"; heldAt = at; }
    public void resume() { require("ON_HOLD"); status = "IN_CONSULTATION"; heldAt = null; }
    public void sign(LocalDateTime at) { require("IN_CONSULTATION"); status = "CONSULTATION_SIGNED"; signedAt = at; }
    private void require(String expected) { if (!expected.equals(status)) throw new IllegalStateException("Encounter must be " + expected + " to perform this action."); }
}
