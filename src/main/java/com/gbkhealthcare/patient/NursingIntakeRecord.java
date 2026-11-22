package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable clinical-intake snapshot. Corrections are represented by a new revision. */
@Entity
@Table(name = "nursing_intake_records", uniqueConstraints = @UniqueConstraint(
        name = "uk_nursing_intake_queue_revision", columnNames = {"queue_entry_id", "revision"}),
        indexes = @Index(name = "idx_nursing_intake_queue_recorded", columnList = "queue_entry_id, recorded_at"))
public class NursingIntakeRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_entry_id", nullable = false)
    private NursingQueueEntry queueEntry;
    @Column(name = "nurse_staff_id", nullable = false)
    private Long nurseStaffId;
    @Column(nullable = false)
    private int revision;
    @Column(name = "bp_systolic") private Integer bpSystolic;
    @Column(name = "bp_diastolic") private Integer bpDiastolic;
    @Column(name = "pulse_rate") private Integer pulseRate;
    @Column(name = "temperature_c", precision = 5, scale = 2) private BigDecimal temperatureC;
    @Column(name = "respiratory_rate") private Integer respiratoryRate;
    @Column(name = "oxygen_saturation") private Integer oxygenSaturation;
    @Column(name = "height_cm", precision = 6, scale = 2) private BigDecimal heightCm;
    @Column(name = "weight_kg", precision = 6, scale = 2) private BigDecimal weightKg;
    @Column(name = "bmi", precision = 7, scale = 2) private BigDecimal bmi;
    @Column(columnDefinition = "TEXT") private String allergies;
    @Column(name = "medication_reconciliation", columnDefinition = "TEXT") private String medicationReconciliation;
    @Column(name = "nursing_notes", columnDefinition = "TEXT") private String nursingNotes;
    @Column(name = "record_state", nullable = false, length = 20) private String recordState;
    @Column(name = "recorded_at", nullable = false, updatable = false) private LocalDateTime recordedAt;
    @Column(name = "signed_at") private LocalDateTime signedAt;

    protected NursingIntakeRecord() {}

    public NursingIntakeRecord(NursingQueueEntry queueEntry, Long nurseStaffId, int revision,
            NursingIntakeRequest request, BigDecimal bmi, String recordState, LocalDateTime at) {
        this.queueEntry = queueEntry; this.nurseStaffId = nurseStaffId; this.revision = revision;
        this.bpSystolic = request.bpSystolic(); this.bpDiastolic = request.bpDiastolic();
        this.pulseRate = request.pulseRate(); this.temperatureC = request.temperatureC();
        this.respiratoryRate = request.respiratoryRate(); this.oxygenSaturation = request.oxygenSaturation();
        this.heightCm = request.heightCm(); this.weightKg = request.weightKg(); this.bmi = bmi;
        this.allergies = clean(request.allergies()); this.medicationReconciliation = clean(request.medicationReconciliation());
        this.nursingNotes = clean(request.nursingNotes()); this.recordState = recordState;
        this.recordedAt = at; this.signedAt = "HANDED_OFF".equals(recordState) ? at : null;
    }
    private static String clean(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    public Long getId() { return id; }
    public NursingQueueEntry getQueueEntry() { return queueEntry; }
    public Long getNurseStaffId() { return nurseStaffId; }
    public int getRevision() { return revision; }
    public Integer getBpSystolic() { return bpSystolic; }
    public Integer getBpDiastolic() { return bpDiastolic; }
    public Integer getPulseRate() { return pulseRate; }
    public BigDecimal getTemperatureC() { return temperatureC; }
    public Integer getRespiratoryRate() { return respiratoryRate; }
    public Integer getOxygenSaturation() { return oxygenSaturation; }
    public BigDecimal getHeightCm() { return heightCm; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getBmi() { return bmi; }
    public String getAllergies() { return allergies; }
    public String getMedicationReconciliation() { return medicationReconciliation; }
    public String getNursingNotes() { return nursingNotes; }
    public String getRecordState() { return recordState; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
}
