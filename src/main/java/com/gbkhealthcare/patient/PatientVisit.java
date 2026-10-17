
package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_visits")
public class PatientVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    private LocalDate visitDate;

    // Scheduled date for the next visit
    private LocalDate followUpDate;

    // Notes specific to the follow-up appointment
    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Column(length = 1000)
    private String chiefComplaints;

    @Column(columnDefinition = "TEXT")
    private String presentIllness;

    @Column(columnDefinition = "TEXT")
    private String generalExamination;

    @Column(columnDefinition = "TEXT")
    private String heartExamination;

    @Column(columnDefinition = "TEXT")
    private String lungsExamination;

    @Column(columnDefinition = "TEXT")
    private String abdomenExamination;

    @Column(columnDefinition = "TEXT")
    private String rectalExamination;

    private Double temperature;

    private Integer pulseRate;

    private Integer spo2;

    private Integer bpSystolic;

    private Integer bpDiastolic;

    @Column(length = 100)
    private String doctor;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "visit_number")
    private Integer visitNumber;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ID
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Patient relationship
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    // Visit date
    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    // Follow-up date
    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    // Follow-up notes
    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    // Chief complaints
    public String getChiefComplaints() {
        return chiefComplaints;
    }

    public void setChiefComplaints(String chiefComplaints) {
        this.chiefComplaints = chiefComplaints;
    }

    // Present illness
    public String getPresentIllness() {
        return presentIllness;
    }

    public void setPresentIllness(String presentIllness) {
        this.presentIllness = presentIllness;
    }

    // General examination
    public String getGeneralExamination() {
        return generalExamination;
    }

    public void setGeneralExamination(String generalExamination) {
        this.generalExamination = generalExamination;
    }

    // Heart examination
    public String getHeartExamination() {
        return heartExamination;
    }

    public void setHeartExamination(String heartExamination) {
        this.heartExamination = heartExamination;
    }

    // Lungs examination
    public String getLungsExamination() {
        return lungsExamination;
    }

    public void setLungsExamination(String lungsExamination) {
        this.lungsExamination = lungsExamination;
    }

    // Abdomen examination
    public String getAbdomenExamination() {
        return abdomenExamination;
    }

    public void setAbdomenExamination(String abdomenExamination) {
        this.abdomenExamination = abdomenExamination;
    }

    // Rectal examination
    public String getRectalExamination() {
        return rectalExamination;
    }

    public void setRectalExamination(String rectalExamination) {
        this.rectalExamination = rectalExamination;
    }

    // Temperature
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    // Pulse rate
    public Integer getPulseRate() {
        return pulseRate;
    }

    public void setPulseRate(Integer pulseRate) {
        this.pulseRate = pulseRate;
    }

    // Oxygen saturation
    public Integer getSpo2() {
        return spo2;
    }

    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    // Blood pressure
    public Integer getBpSystolic() {
        return bpSystolic;
    }

    public void setBpSystolic(Integer bpSystolic) {
        this.bpSystolic = bpSystolic;
    }

    public Integer getBpDiastolic() {
        return bpDiastolic;
    }

    public void setBpDiastolic(Integer bpDiastolic) {
        this.bpDiastolic = bpDiastolic;
    }

    // Doctor
    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    // Diagnosis
    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    // Treatment plan
    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }

    // Creation timestamp
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getVisitNumber() {
        return visitNumber;
    }

    public void setVisitNumber(Integer visitNumber) {
        this.visitNumber = visitNumber;
    }
}
