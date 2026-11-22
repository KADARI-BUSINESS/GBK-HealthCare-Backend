
package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_follow_ups", indexes =
        @Index(name = "idx_patient_follow_ups_status_date", columnList = "status, follow_up_date"))
public class PatientFollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private PatientVisit visit;

    @OneToOne(mappedBy = "sourceFollowUp", fetch = FetchType.LAZY)
    private PatientVisit linkedVisit;

    @Column(name = "follow_up_date", nullable = false)
    private LocalDate followUpDate;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    private String followUpNotes;

    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Column(name = "checked_in_at") private LocalDateTime checkedInAt;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
    @Column(name = "cancelled_by_actor_type", length = 16) private String cancelledByActorType;
    @Column(name = "cancelled_by_actor_id") private Long cancelledByActorId;
    @Column(name = "cancellation_reason", length = 1000) private String cancellationReason;
    @Column(name = "missed_at") private LocalDateTime missedAt;
    @Column(name = "missed_by_actor_type", length = 16) private String missedByActorType;
    @Column(name = "missed_by_actor_id") private Long missedByActorId;
    @Column(name = "missed_reason", length = 1000) private String missedReason;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "SCHEDULED";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientVisit getVisit() {
        return visit;
    }

    public void setVisit(PatientVisit visit) {
        this.visit = visit;
    }
    public PatientVisit getLinkedVisit() { return linkedVisit; }
    public void setLinkedVisit(PatientVisit linkedVisit) { this.linkedVisit = linkedVisit; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }
    public LocalDateTime getCheckedInAt() { return checkedInAt; }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getFollowUpNotes() {
        return followUpNotes;
    }

    public void setFollowUpNotes(String followUpNotes) {
        this.followUpNotes = followUpNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancelledByActorType() { return cancelledByActorType; }
    public Long getCancelledByActorId() { return cancelledByActorId; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getMissedAt() { return missedAt; }
    public String getMissedByActorType() { return missedByActorType; }
    public Long getMissedByActorId() { return missedByActorId; }
    public String getMissedReason() { return missedReason; }
    public void markInProgress(LocalDateTime at) {
        if (!"SCHEDULED".equals(status)) throw new IllegalStateException("Only scheduled follow-ups can enter consultation.");
        status = "IN_PROGRESS"; startedAt = at;
    }
    public void markCompleted(LocalDateTime at) {
        if ("COMPLETED".equals(status)) return;
        if (!"IN_PROGRESS".equals(status)) throw new IllegalStateException("A follow-up can be completed only after its consultation starts.");
        status = "COMPLETED"; completedAt = at;
    }
    public void markCancelled(String actorType, Long actorId, String reason, LocalDateTime at) {
        if (!"SCHEDULED".equals(status)) throw new IllegalStateException("Only scheduled follow-ups can be cancelled.");
        status = "CANCELLED"; cancelledByActorType = actorType; cancelledByActorId = actorId;
        cancellationReason = reason; cancelledAt = at;
    }
    public void markMissed(String actorType, Long actorId, String reason, LocalDateTime at) {
        if (!"SCHEDULED".equals(status)) throw new IllegalStateException("Only scheduled follow-ups can be marked missed.");
        status = "MISSED"; missedByActorType = actorType; missedByActorId = actorId; missedReason = reason; missedAt = at;
    }
}
