package com.gbkhealthcare.patient;

import java.time.LocalDate;

public record ReceptionFollowUpOption(Long followUpId, LocalDate followUpDate) {
    public static ReceptionFollowUpOption from(PatientFollowUp followUp) {
        return new ReceptionFollowUpOption(followUp.getId(), followUp.getFollowUpDate());
    }
}
