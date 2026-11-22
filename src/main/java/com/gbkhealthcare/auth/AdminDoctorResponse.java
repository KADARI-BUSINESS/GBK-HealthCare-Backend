package com.gbkhealthcare.auth;

import java.time.LocalDateTime;

/** Safe Admin-facing doctor fields; authentication secrets are intentionally excluded. */
public record AdminDoctorResponse(
        Long id,
        String fullName,
        String email,
        String mobileNumber,
        String qualification,
        String specialization,
        String medicalRegistrationNumber,
        Doctor.ApprovalStatus approvalStatus,
        boolean enabled,
        LocalDateTime createdAt) {

    public static AdminDoctorResponse from(Doctor doctor) {
        return new AdminDoctorResponse(doctor.getId(), doctor.getFullName(), doctor.getEmail(),
                doctor.getMobileNumber(), doctor.getQualification(), doctor.getSpecialization(),
                doctor.getMedicalRegistrationNumber(),
                doctor.getApprovalStatus(), doctor.isEnabled(), doctor.getCreatedAt());
    }
}
