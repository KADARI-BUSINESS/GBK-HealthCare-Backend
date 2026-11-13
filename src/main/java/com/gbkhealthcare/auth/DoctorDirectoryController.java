package com.gbkhealthcare.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/doctors")
public class DoctorDirectoryController {
    private final DoctorRepository doctorRepository;

    public DoctorDirectoryController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
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
