package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardStatsController {
    private final DashboardStatsService dashboardStatsService;
    private final DoctorRepository doctorRepository;

    public DashboardStatsController(
            DashboardStatsService dashboardStatsService,
            DoctorRepository doctorRepository) {
        this.dashboardStatsService = dashboardStatsService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/stats")
    public DashboardStatsResponse getStats(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
        }

        Doctor doctor = doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));

        return dashboardStatsService.getStats(doctor.getId());
    }
}
