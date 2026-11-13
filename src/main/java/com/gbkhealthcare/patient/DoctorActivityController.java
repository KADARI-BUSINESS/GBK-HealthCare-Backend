package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DoctorActivityController {
    private final DoctorActivityService activityService;
    private final DoctorRepository doctorRepository;

    public DoctorActivityController(
            DoctorActivityService activityService,
            DoctorRepository doctorRepository) {
        this.activityService = activityService;
        this.doctorRepository = doctorRepository;
    }

    @GetMapping("/activity")
    public List<DoctorActivityResponse> getActivity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "8") int limit) {
        if (limit < 1 || limit > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Activity limit must be between 1 and 20.");
        }
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication is required.");
        }
        Doctor doctor = doctorRepository.findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Active doctor account not found."));
        return activityService.getRecentActivities(doctor.getId(), limit);
    }
}
