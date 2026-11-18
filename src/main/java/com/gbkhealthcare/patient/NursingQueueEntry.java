package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "nursing_queue_entries", uniqueConstraints =
        @UniqueConstraint(name = "uk_nursing_queue_patient_visit", columnNames = "patient_visit_id"),
        indexes = {
                @Index(name = "idx_nursing_queue_status_created", columnList = "status, created_at"),
                @Index(name = "idx_nursing_queue_claimed_staff", columnList = "claimed_by_staff_id, status")})
public class NursingQueueEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_visit_id", nullable = false)
    private PatientVisit visit;

    @Column(nullable = false, length = 24)
    private String status = "WAITING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "claimed_by_staff_id")
    private Long claimedByStaffId;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "handed_off_at")
    private LocalDateTime handedOffAt;

    protected NursingQueueEntry() {}

    public NursingQueueEntry(PatientVisit visit) {
        this.visit = visit;
        this.status = "WAITING";
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }

    public Long getId() { return id; }
    public PatientVisit getVisit() { return visit; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getClaimedByStaffId() { return claimedByStaffId; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public LocalDateTime getHandedOffAt() { return handedOffAt; }

    public void claim(Long nurseId, LocalDateTime at) {
        if (!"WAITING".equals(status)) throw new IllegalStateException("Only waiting queue entries can be claimed.");
        status = "CLAIMED";
        claimedByStaffId = nurseId;
        claimedAt = at;
    }

    public void handoff(LocalDateTime at) {
        if (!"CLAIMED".equals(status)) throw new IllegalStateException("Only claimed queue entries can be handed off.");
        status = "HANDED_OFF";
        handedOffAt = at;
    }
}
