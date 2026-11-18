package com.gbkhealthcare.patient;

import java.time.LocalDate;

public record RelevantHistoryItemResponse(Long visitId, LocalDate visitDate, String chiefComplaints,
        String presentIllness, String generalExamination, String heartExamination,
        String lungsExamination, String abdomenExamination, String diagnosis,
        String treatmentPlan, Double temperatureC, Integer pulseRate, Integer oxygenSaturation,
        Integer bpSystolic, Integer bpDiastolic) {
    static RelevantHistoryItemResponse from(PatientVisit v) {
        return new RelevantHistoryItemResponse(v.getId(), v.getVisitDate(), v.getChiefComplaints(), v.getPresentIllness(),
                v.getGeneralExamination(), v.getHeartExamination(), v.getLungsExamination(), v.getAbdomenExamination(),
                v.getDiagnosis(), v.getTreatmentPlan(), v.getTemperature(), v.getPulseRate(), v.getSpo2(),
                v.getBpSystolic(), v.getBpDiastolic());
    }
}
