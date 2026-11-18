package com.gbkhealthcare.patient;

import java.time.LocalDate;

public record ReceptionCheckInResponse(Long patientId, String patientName, Long visitId,
        Integer visitNumber, EncounterType encounterType, Long assignedDoctorId,
        String assignedDoctorName, LocalDate visitDate, boolean emergencyPriority,
        Long sourceFollowUpId, String nursingQueueStatus) {
    public static ReceptionCheckInResponse from(PatientVisit visit, NursingQueueEntry queue) {
        return new ReceptionCheckInResponse(visit.getPatient().getId(), visit.getPatient().getFullName(),
                visit.getId(), visit.getVisitNumber(), visit.getEncounterType(), visit.getAssignedDoctor().getId(),
                visit.getAssignedDoctor().getFullName(), visit.getVisitDate(), visit.isEmergencyPriority(),
                visit.getSourceFollowUp() == null ? null : visit.getSourceFollowUp().getId(), queue.getStatus());
    }
}
