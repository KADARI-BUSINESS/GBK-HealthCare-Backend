package com.gbkhealthcare.patient;

import java.time.LocalDateTime;

public record DoctorConsultationResponse(Long queueEntryId, Long patientVisitId, String encounterStatus,
        String consultationStatus, String chiefComplaints, String presentIllness, String generalExamination,
        String heartExamination, String lungsExamination, String abdomenExamination, String rectalExamination,
        String diagnosis, String assessment, String treatmentPlan, String doctorNotes,
        LocalDateTime signedAt, String checkoutStatus) {
    static DoctorConsultationResponse from(DoctorQueueEntry q) {
        PatientVisit v = q.getPatientVisit();
        return new DoctorConsultationResponse(q.getId(), v.getId(), v.getEncounterStatus(), v.getConsultationStatus(),
                v.getChiefComplaints(), v.getPresentIllness(), v.getGeneralExamination(), v.getHeartExamination(),
                v.getLungsExamination(), v.getAbdomenExamination(), v.getRectalExamination(), v.getDiagnosis(),
                v.getAssessment(), v.getTreatmentPlan(), v.getDoctorNotes(), v.getConsultationSignedAt(), v.getCheckoutStatus());
    }
}
