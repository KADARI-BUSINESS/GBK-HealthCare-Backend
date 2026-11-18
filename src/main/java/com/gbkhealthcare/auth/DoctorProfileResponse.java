package com.gbkhealthcare.auth;

/** Minimal authenticated profile projection; contains no authentication secrets. */
public record DoctorProfileResponse(Long doctorId, String fullName, String email) {
}
