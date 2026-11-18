package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="clinical_order_audit_events", indexes=@Index(name="idx_clinical_order_audit_visit_time", columnList="patient_visit_id, occurred_at"))
public class ClinicalOrderAuditEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="doctor_id", nullable=false, updatable=false) private Long doctorId;
    @Column(name="patient_visit_id", nullable=false, updatable=false) private Long patientVisitId;
    @Column(name="resource_type", nullable=false, length=24, updatable=false) private String resourceType;
    @Column(name="resource_id", nullable=false, updatable=false) private Long resourceId;
    @Column(name="action_type", nullable=false, length=32, updatable=false) private String actionType;
    @Column(name="occurred_at", nullable=false, updatable=false) private LocalDateTime occurredAt;
    protected ClinicalOrderAuditEvent() {}
    public ClinicalOrderAuditEvent(Long doctorId, Long visitId, String resourceType, Long resourceId, String action, LocalDateTime at){this.doctorId=doctorId;this.patientVisitId=visitId;this.resourceType=resourceType;this.resourceId=resourceId;this.actionType=action;this.occurredAt=at;}
    public Long getId(){return id;} public Long getDoctorId(){return doctorId;} public Long getPatientVisitId(){return patientVisitId;} public String getResourceType(){return resourceType;} public Long getResourceId(){return resourceId;} public String getActionType(){return actionType;} public LocalDateTime getOccurredAt(){return occurredAt;}
}
