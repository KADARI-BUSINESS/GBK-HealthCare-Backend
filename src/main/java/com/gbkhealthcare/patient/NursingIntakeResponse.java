package com.gbkhealthcare.patient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NursingIntakeResponse(Long recordId, Long queueEntryId, Long nurseStaffId, int revision,
        Integer bpSystolic, Integer bpDiastolic, Integer pulseRate, BigDecimal temperatureC,
        Integer respiratoryRate, Integer oxygenSaturation, BigDecimal heightCm, BigDecimal weightKg,
        BigDecimal bmi, String allergies, String medicationReconciliation, String nursingNotes,
        String recordState, LocalDateTime recordedAt, LocalDateTime signedAt) {
    public static NursingIntakeResponse from(NursingIntakeRecord r) {
        return new NursingIntakeResponse(r.getId(), r.getQueueEntry().getId(), r.getNurseStaffId(), r.getRevision(),
                r.getBpSystolic(), r.getBpDiastolic(), r.getPulseRate(), r.getTemperatureC(), r.getRespiratoryRate(),
                r.getOxygenSaturation(), r.getHeightCm(), r.getWeightKg(), r.getBmi(), r.getAllergies(),
                r.getMedicationReconciliation(), r.getNursingNotes(), r.getRecordState(), r.getRecordedAt(), r.getSignedAt());
    }
}
