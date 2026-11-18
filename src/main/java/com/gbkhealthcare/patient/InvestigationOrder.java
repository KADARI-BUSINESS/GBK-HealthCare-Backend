package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="investigation_orders", indexes={@Index(name="idx_investigation_visit_status", columnList="patient_visit_id, status"), @Index(name="idx_investigation_doctor_time", columnList="doctor_id, ordered_at")})
public class InvestigationOrder {
    public enum Type { LAB, IMAGING, OTHER }
    public enum Priority { ROUTINE, URGENT, STAT }
    public enum Status { ORDERED, IN_PROGRESS, COMPLETED, CANCELLED }
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="patient_visit_id", nullable=false, updatable=false) private PatientVisit patientVisit;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="doctor_id", nullable=false, updatable=false) private Doctor doctor;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private Type type;
    @Column(nullable=false, length=180) private String name;
    @Column(columnDefinition="TEXT") private String indication;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16) private Priority priority;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status=Status.ORDERED;
    @Column(name="ordered_at", nullable=false, updatable=false) private LocalDateTime orderedAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    protected InvestigationOrder() {}
    public InvestigationOrder(PatientVisit visit, Doctor doctor, Type type, String name, String indication, Priority priority, LocalDateTime at) { this.patientVisit=visit; this.doctor=doctor; this.type=type; this.name=name.trim(); this.indication=clean(indication); this.priority=priority; this.orderedAt=at; this.updatedAt=at; }
    public void changeStatus(Status next, LocalDateTime at) { boolean valid=(status==Status.ORDERED && (next==Status.IN_PROGRESS || next==Status.CANCELLED)) || (status==Status.IN_PROGRESS && (next==Status.COMPLETED || next==Status.CANCELLED)); if(!valid) throw new IllegalStateException("Investigation status transition is not allowed."); status=next; updatedAt=at; }
    private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
    public Long getId(){return id;} public PatientVisit getPatientVisit(){return patientVisit;} public Doctor getDoctor(){return doctor;} public Type getType(){return type;} public String getName(){return name;} public String getIndication(){return indication;} public Priority getPriority(){return priority;} public Status getStatus(){return status;} public LocalDateTime getOrderedAt(){return orderedAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
