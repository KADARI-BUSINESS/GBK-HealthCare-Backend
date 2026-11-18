package com.gbkhealthcare.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record FollowUpLifecycleResponse(Long followUpId, Long patientId, String patientName,
        Long originalVisitId, Long checkedInVisitId, Long assignedDoctorId, String assignedDoctorName,
        LocalDate followUpDate, String status, LocalDateTime checkedInAt, LocalDateTime startedAt,
        LocalDateTime completedAt, LocalDateTime cancelledAt, String cancellationReason,
        LocalDateTime missedAt, String missedReason) {
    public static FollowUpLifecycleResponse from(PatientFollowUp f) {
        PatientVisit original = f.getVisit();
        PatientVisit linked = f.getLinkedVisit();
        return new FollowUpLifecycleResponse(f.getId(), original.getPatient().getId(), original.getPatient().getFullName(),
                original.getId(), linked == null ? null : linked.getId(),
                linked == null || linked.getAssignedDoctor() == null ? null : linked.getAssignedDoctor().getId(),
                linked == null ? null : linked.getAssignedDoctor().getFullName(), f.getFollowUpDate(), f.getStatus(),
                f.getCheckedInAt(), f.getStartedAt(), f.getCompletedAt(), f.getCancelledAt(), f.getCancellationReason(),
                f.getMissedAt(), f.getMissedReason());
    }
}
