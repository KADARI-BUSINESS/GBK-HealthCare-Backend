package com.gbkhealthcare.patient;

import java.time.LocalDateTime;

public record ConsultationAddendumResponse(Long id, Long patientVisitId, Long doctorId, String text, LocalDateTime createdAt) {
    static ConsultationAddendumResponse from(ConsultationAddendum a) {
        return new ConsultationAddendumResponse(a.getId(), a.getPatientVisit().getId(), a.getDoctorId(), a.getAddendumText(), a.getCreatedAt());
    }
}
