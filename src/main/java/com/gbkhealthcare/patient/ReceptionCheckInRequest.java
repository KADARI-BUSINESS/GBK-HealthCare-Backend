package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReceptionCheckInRequest(
        @NotNull EncounterType encounterType,
        @NotNull @Positive Long assignedDoctorId,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F-]{36}", message = "A UUID request key is required.") String requestKey,
        @Positive Long patientId,
        @Valid PatientController.PatientRequest newPatient,
        @Positive Long followUpId,
        boolean emergencyPriority,
        @Size(max = 1500) String emergencyNote) {

    @AssertTrue(message = "Provide exactly one of patientId or newPatient.")
    public boolean isPatientSourceValid() {
        return (patientId != null) != (newPatient != null);
    }

    @AssertTrue(message = "New patients must start a New Visit; Follow-up Visits require a scheduled follow-up.")
    public boolean isVisitTypeValid() {
        if (newPatient != null) return encounterType == EncounterType.NEW && followUpId == null;
        return encounterType == EncounterType.FOLLOW_UP ? followUpId != null : followUpId == null;
    }

    @AssertTrue(message = "An emergency note can only be supplied for emergency-priority visits.")
    public boolean isEmergencyNoteValid() {
        return emergencyPriority || emergencyNote == null || emergencyNote.isBlank();
    }
}
