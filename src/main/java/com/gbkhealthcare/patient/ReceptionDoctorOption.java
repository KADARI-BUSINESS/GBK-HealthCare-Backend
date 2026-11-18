package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;

public record ReceptionDoctorOption(Long doctorId, String fullName, String specialization) {
    public static ReceptionDoctorOption from(Doctor doctor) {
        return new ReceptionDoctorOption(doctor.getId(), doctor.getFullName(), doctor.getSpecialization());
    }
}
