package com.gbkhealthcare.auth;

import java.util.List;
import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/doctors")
public class DoctorDirectoryController {
    private final DoctorRepository doctorRepository;

    public DoctorDirectoryController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/me")
    public DoctorProfileResponse getCurrentDoctorProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        Long doctorId = null;
        try {
            Object claim = jwt.getClaims().get("doctorId");
            if (claim instanceof Number number) {
                doctorId = new BigDecimal(number.toString()).longValueExact();
            }
        } catch (RuntimeException invalidClaim) {
            // Treat malformed, fractional or out-of-range identity claims as unauthenticated.
        }
        if (doctorId == null || doctorId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid Doctor identity is required.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .filter(Doctor::isEnabled)
                .filter(candidate -> candidate.getApprovalStatus() == Doctor.ApprovalStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Active approved Doctor account not found."));

        return new DoctorProfileResponse(doctor.getId(), doctor.getFullName(), doctor.getEmail());
    }

    @GetMapping
    public List<DoctorDirectoryEntry> listDoctors(@AuthenticationPrincipal Jwt jwt) {
        doctorRepository.findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Active doctor account not found."));
        return doctorRepository.findAllDirectoryEntries();
    }
}
