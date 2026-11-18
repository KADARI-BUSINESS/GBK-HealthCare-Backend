package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="referral_orders", indexes={@Index(name="idx_referral_visit_status", columnList="patient_visit_id, status"), @Index(name="idx_referral_doctor_time", columnList="doctor_id, created_at")})
public class ReferralOrder {
    public enum Priority { ROUTINE, URGENT, STAT }
    public enum Status { CREATED, SENT, ACCEPTED, COMPLETED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="patient_visit_id", nullable=false, updatable=false) private PatientVisit patientVisit;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="doctor_id", nullable=false, updatable=false) private Doctor doctor;
    @Column(nullable=false, length=160) private String specialty;
    @Column(name="provider_or_facility", length=200) private String providerOrFacility;
    @Column(name="clinical_reason", nullable=false, columnDefinition="TEXT") private String clinicalReason;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private Priority priority;
    @Column(columnDefinition="TEXT") private String instructions;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status=Status.CREATED;
    @Column(name="created_at", nullable=false, updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    protected ReferralOrder() {}
    public ReferralOrder(PatientVisit visit, Doctor doctor, String specialty, String provider, String reason, Priority priority, String instructions, LocalDateTime at){this.patientVisit=visit;this.doctor=doctor;this.specialty=specialty.trim();this.providerOrFacility=clean(provider);this.clinicalReason=reason.trim();this.priority=priority;this.instructions=clean(instructions);this.createdAt=at;this.updatedAt=at;}
    public void changeStatus(Status next, LocalDateTime at){boolean valid=(status==Status.CREATED&&(next==Status.SENT||next==Status.CANCELLED))||(status==Status.SENT&&(next==Status.ACCEPTED||next==Status.COMPLETED||next==Status.CANCELLED))||(status==Status.ACCEPTED&&(next==Status.COMPLETED||next==Status.CANCELLED));if(!valid)throw new IllegalStateException("Referral status transition is not allowed.");status=next;updatedAt=at;}
    private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    public Long getId(){return id;} public PatientVisit getPatientVisit(){return patientVisit;} public Doctor getDoctor(){return doctor;} public String getSpecialty(){return specialty;} public String getProviderOrFacility(){return providerOrFacility;} public String getClinicalReason(){return clinicalReason;} public Priority getPriority(){return priority;} public String getInstructions(){return instructions;} public Status getStatus(){return status;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
