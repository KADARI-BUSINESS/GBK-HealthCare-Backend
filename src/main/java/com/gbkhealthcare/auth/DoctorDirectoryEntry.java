package com.gbkhealthcare.auth;

/** Public directory fields only; never includes authentication secrets. */
public record DoctorDirectoryEntry(Long id, String fullName, String email, boolean enabled) {}
