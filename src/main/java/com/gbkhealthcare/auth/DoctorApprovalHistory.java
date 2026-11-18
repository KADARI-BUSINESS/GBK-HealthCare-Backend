package com.gbkhealthcare.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_approval_history", indexes = {
        @Index(name = "idx_doctor_approval_history_decision", columnList = "decision_at, id"),
        @Index(name = "idx_doctor_approval_history_doctor", columnList = "doctor_id, decision_at, id")
})
public class DoctorApprovalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false, updatable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_approval_status", nullable = false, length = 20, updatable = false)
    private Doctor.ApprovalStatus previousApprovalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_approval_status", nullable = false, length = 20, updatable = false)
    private Doctor.ApprovalStatus newApprovalStatus;

    @Column(name = "admin_id", nullable = false, updatable = false)
    private Long adminId;

    @Column(name = "admin_email", nullable = false, length = 160, updatable = false)
    private String adminEmail;

    @Column(name = "decision_at", nullable = false, updatable = false)
    private LocalDateTime decisionAt;

    @Column(name = "rejection_reason", length = 500, updatable = false)
    private String rejectionReason;

    protected DoctorApprovalHistory() {
    }

    public DoctorApprovalHistory(Doctor doctor, Doctor.ApprovalStatus previousApprovalStatus,
            Doctor.ApprovalStatus newApprovalStatus, Long adminId, String adminEmail,
            LocalDateTime decisionAt, String rejectionReason) {
        this.doctor = doctor;
        this.previousApprovalStatus = previousApprovalStatus;
        this.newApprovalStatus = newApprovalStatus;
        this.adminId = adminId;
        this.adminEmail = adminEmail;
        this.decisionAt = decisionAt;
        this.rejectionReason = rejectionReason;
    }

    @PrePersist
    void onCreate() {
        if (decisionAt == null) decisionAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public Doctor.ApprovalStatus getPreviousApprovalStatus() { return previousApprovalStatus; }
    public Doctor.ApprovalStatus getNewApprovalStatus() { return newApprovalStatus; }
    public Long getAdminId() { return adminId; }
    public String getAdminEmail() { return adminEmail; }
    public LocalDateTime getDecisionAt() { return decisionAt; }
    public String getRejectionReason() { return rejectionReason; }
}
