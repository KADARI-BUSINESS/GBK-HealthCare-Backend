package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions", uniqueConstraints = @UniqueConstraint(name = "uk_prescription_visit_revision", columnNames = {"patient_visit_id", "revision"}),
        indexes = @Index(name = "idx_prescription_visit_status", columnList = "patient_visit_id, status"))
public class Prescription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_visit_id", nullable = false) private PatientVisit patientVisit;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "doctor_id", nullable = false) private Doctor doctor;
    @Column(nullable = false) private int revision;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private State status = State.DRAFT;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supersedes_prescription_id") private Prescription supersedes;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "signed_at") private LocalDateTime signedAt;
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC") private List<PrescriptionItem> items = new ArrayList<>();
    public enum State { DRAFT, SIGNED }
    protected Prescription() {}
    public Prescription(PatientVisit visit, Doctor doctor, int revision, Prescription supersedes, String notes, LocalDateTime createdAt) {
        patientVisit = visit; this.doctor = doctor; this.revision = revision; this.supersedes = supersedes; this.notes = clean(notes); this.createdAt = createdAt;
    }
    public void replaceItems(List<PrescriptionItem> replacement) {
        if (status != State.DRAFT) throw new IllegalStateException("Signed prescriptions are immutable.");
        items.clear();
        for (int order = 0; order < replacement.size(); order++) {
            PrescriptionItem item = replacement.get(order);
            item.setItemOrder(order);
            item.setPrescription(this);
            items.add(item);
        }
    }
    public void updateNotes(String value) { if (status != State.DRAFT) throw new IllegalStateException("Signed prescriptions are immutable."); notes = clean(value); }
    public void sign(LocalDateTime at) { if (status != State.DRAFT) throw new IllegalStateException("Only draft prescriptions can be signed."); status = State.SIGNED; signedAt = at; }
    private static String clean(String v) { return v == null || v.isBlank() ? null : v.trim(); }
    public Long getId() { return id; } public PatientVisit getPatientVisit() { return patientVisit; } public Doctor getDoctor() { return doctor; }
    public int getRevision() { return revision; } public State getStatus() { return status; } public Prescription getSupersedes() { return supersedes; }
    public String getNotes() { return notes; } public LocalDateTime getCreatedAt() { return createdAt; } public LocalDateTime getSignedAt() { return signedAt; }
    public List<PrescriptionItem> getItems() { return List.copyOf(items); }
}
