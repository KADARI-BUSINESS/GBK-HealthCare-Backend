
package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_visits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patient_visits_follow_up", columnNames = "source_follow_up_id"),
        @UniqueConstraint(name = "uk_patient_visits_check_in_key", columnNames = "check_in_request_key")
}, indexes = {
        @Index(name = "idx_patient_visits_assigned_doctor_date", columnList = "assigned_doctor_id, visit_date"),
        @Index(name = "idx_patient_visits_checkout_complete", columnList = "checkout_status, checkout_completed_at")
})
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

    // Reception/OPD additions. Legacy Patient.doctorAccount remains untouched.
    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_type", length = 20)
    private EncounterType encounterType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private Doctor assignedDoctor;

    @Column(name = "checked_in_by_staff_id")
    private Long checkedInByStaffId;

    @JdbcTypeCode(Types.BIT)
    @Column(name = "emergency_priority", nullable = false)
    private boolean emergencyPriority;

    @Column(name = "emergency_note", columnDefinition = "TEXT")
    private String emergencyNote;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_follow_up_id")
    private PatientFollowUp sourceFollowUp;

    @Column(name = "check_in_request_key", length = 36)
    private String checkInRequestKey;

    // OPD lifecycle fields are nullable for unchanged legacy PatientVisit records.
    @Column(name = "encounter_status", length = 32)
    private String encounterStatus;
    @Column(name = "consultation_status", length = 16)
    private String consultationStatus;
    @Column(columnDefinition = "TEXT")
    private String assessment;
    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;
    @Column(name = "consultation_signed_at")
    private LocalDateTime consultationSignedAt;
    @Column(name = "checkout_status", length = 24)
    private String checkoutStatus;
    @Column(name = "checkout_completed_at") private LocalDateTime checkoutCompletedAt;
    @Column(name = "checkout_completed_by_staff_id") private Long checkoutCompletedByStaffId;

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

    public EncounterType getEncounterType() { return encounterType; }
    public void setEncounterType(EncounterType encounterType) { this.encounterType = encounterType; }
    public Doctor getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(Doctor assignedDoctor) { this.assignedDoctor = assignedDoctor; }
    public Long getCheckedInByStaffId() { return checkedInByStaffId; }
    public void setCheckedInByStaffId(Long checkedInByStaffId) { this.checkedInByStaffId = checkedInByStaffId; }
    public boolean isEmergencyPriority() { return emergencyPriority; }
    public void setEmergencyPriority(boolean emergencyPriority) { this.emergencyPriority = emergencyPriority; }
    public String getEmergencyNote() { return emergencyNote; }
    public void setEmergencyNote(String emergencyNote) { this.emergencyNote = emergencyNote; }
    public PatientFollowUp getSourceFollowUp() { return sourceFollowUp; }
    public void setSourceFollowUp(PatientFollowUp sourceFollowUp) { this.sourceFollowUp = sourceFollowUp; }
    public String getCheckInRequestKey() { return checkInRequestKey; }
    public void setCheckInRequestKey(String checkInRequestKey) { this.checkInRequestKey = checkInRequestKey; }
    public String getEncounterStatus() { return encounterStatus; }
    public void setEncounterStatus(String encounterStatus) { this.encounterStatus = encounterStatus; }
    public String getConsultationStatus() { return consultationStatus; }
    public void setConsultationStatus(String consultationStatus) { this.consultationStatus = consultationStatus; }
    public String getAssessment() { return assessment; }
    public void setAssessment(String assessment) { this.assessment = assessment; }
    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
    public LocalDateTime getConsultationSignedAt() { return consultationSignedAt; }
    public void setConsultationSignedAt(LocalDateTime consultationSignedAt) { this.consultationSignedAt = consultationSignedAt; }
    public String getCheckoutStatus() { return checkoutStatus; }
    public void setCheckoutStatus(String checkoutStatus) { this.checkoutStatus = checkoutStatus; }
    public LocalDateTime getCheckoutCompletedAt() { return checkoutCompletedAt; }
    public void setCheckoutCompletedAt(LocalDateTime checkoutCompletedAt) { this.checkoutCompletedAt = checkoutCompletedAt; }
    public Long getCheckoutCompletedByStaffId() { return checkoutCompletedByStaffId; }
    public void setCheckoutCompletedByStaffId(Long checkoutCompletedByStaffId) { this.checkoutCompletedByStaffId = checkoutCompletedByStaffId; }
}
