package com.gbkhealthcare.patient;

import java.time.Duration;
import java.time.LocalDateTime;

public record NursingQueueItemResponse(Long queueEntryId, Long patientVisitId, Long patientId,
        String patientName, Integer patientAge, String patientGender, String patientMobile, Long assignedDoctorId,
        String assignedDoctorName, LocalDateTime checkedInAt, boolean emergencyPriority,
        String emergencyNote, String status, Long claimedByStaffId, LocalDateTime claimedAt,
        long waitingSeconds) {
    public static NursingQueueItemResponse from(NursingQueueEntry q, LocalDateTime now) {
        PatientVisit v = q.getVisit(); Patient p = v.getPatient();
        return new NursingQueueItemResponse(q.getId(), v.getId(), p.getId(), p.getFullName(), p.getAge(), p.getGender(), p.getPhone(),
                v.getAssignedDoctor() == null ? null : v.getAssignedDoctor().getId(),
                v.getAssignedDoctor() == null ? v.getDoctor() : v.getAssignedDoctor().getFullName(),
                q.getCreatedAt(), v.isEmergencyPriority(), v.getEmergencyNote(), q.getStatus(), q.getClaimedByStaffId(),
                q.getClaimedAt(), Math.max(0, Duration.between(q.getCreatedAt(), now).getSeconds()));
    }
}
