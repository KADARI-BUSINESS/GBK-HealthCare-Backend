package com.gbkhealthcare.patient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;

public record DoctorOpdQueueItemResponse(Long queueEntryId, Long patientVisitId, Long patientId,
        String patientName, Integer patientAge, String patientGender, String patientMobile,
        LocalDateTime checkedInAt, boolean emergencyPriority, String emergencyNote,
        String status, long waitingSeconds, LocalDateTime claimedAt, LocalDateTime consultationStartedAt,
        LocalDateTime heldAt, EncounterType encounterType, Long followUpId, LocalDate followUpDate,
        String followUpStatus) {
    public static DoctorOpdQueueItemResponse from(DoctorQueueEntry q, LocalDateTime now) {
        PatientVisit v = q.getPatientVisit(); Patient p = v.getPatient();
        return new DoctorOpdQueueItemResponse(q.getId(), v.getId(), p.getId(), p.getFullName(), p.getAge(), p.getGender(), p.getPhone(),
                q.getCreatedAt(), v.isEmergencyPriority(), v.getEmergencyNote(), q.getStatus(),
                Math.max(0, Duration.between(q.getCreatedAt(), now).getSeconds()), q.getClaimedAt(),
                q.getConsultationStartedAt(), q.getHeldAt(), v.getEncounterType(),
                v.getSourceFollowUp() == null ? null : v.getSourceFollowUp().getId(),
                v.getSourceFollowUp() == null ? null : v.getSourceFollowUp().getFollowUpDate(),
                v.getSourceFollowUp() == null ? null : v.getSourceFollowUp().getStatus());
    }
}
