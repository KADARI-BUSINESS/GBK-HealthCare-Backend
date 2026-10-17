
package com.gbkhealthcare.patient;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String message) {
        super(message);
    }

    public PatientNotFoundException(Long patientId) {
        super("Patient with ID " + patientId + " was not found.");
    }
}
