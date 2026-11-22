package com.gbkhealthcare.patient;

import java.time.LocalDate;

public record ReceptionPatientSummary(Long patientId, String fullName, Integer age,
        String gender, String mobileNumber, String email, LocalDate registeredVisitDate) {
    public static ReceptionPatientSummary from(Patient patient) {
        return new ReceptionPatientSummary(patient.getId(), patient.getFullName(), patient.getAge(),
                patient.getGender(), patient.getPhone(), patient.getEmail(), patient.getVisitDate());
    }
}
