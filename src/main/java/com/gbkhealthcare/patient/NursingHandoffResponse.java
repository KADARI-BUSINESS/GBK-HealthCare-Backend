package com.gbkhealthcare.patient;

import java.time.LocalDateTime;

public record NursingHandoffResponse(Long doctorQueueEntryId, Long patientVisitId, Long assignedDoctorId,
        String status, LocalDateTime handedOffAt) {}
