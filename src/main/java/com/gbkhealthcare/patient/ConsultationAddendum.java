package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation_addenda", indexes = @Index(name = "idx_consult_addenda_visit_created", columnList = "patient_visit_id, created_at"))
public class ConsultationAddendum {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_visit_id", nullable = false) private PatientVisit patientVisit;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "addendum_text", nullable = false, columnDefinition = "TEXT") private String addendumText;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    protected ConsultationAddendum() {}
    public ConsultationAddendum(PatientVisit visit, Long doctorId, String text, LocalDateTime at) { patientVisit = visit; this.doctorId = doctorId; addendumText = text; createdAt = at; }
    public Long getId() { return id; }
    public PatientVisit getPatientVisit() { return patientVisit; }
    public Long getDoctorId() { return doctorId; }
    public String getAddendumText() { return addendumText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
