package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorActivityControllerTest {
    private DoctorActivityService activityService;
    private DoctorRepository doctorRepository;
    private DoctorActivityController controller;

    @BeforeEach
    void setUp() {
        activityService = mock(DoctorActivityService.class);
        doctorRepository = mock(DoctorRepository.class);
        controller = new DoctorActivityController(activityService, doctorRepository);
    }

    @Test
    void derivesScopeFromAuthenticatedDoctorAndDefaultsToEight() {
        Doctor doctorA = doctor(4L, true);
        when(doctorRepository.findByEmailIgnoreCase("a@example.com"))
                .thenReturn(Optional.of(doctorA));
        when(activityService.getRecentActivities(4L, 8)).thenReturn(List.of());

        assertEquals(List.of(), controller.getActivity(jwt("a@example.com"), 8));
        verify(activityService).getRecentActivities(4L, 8);
        verify(activityService, never()).getRecentActivities(9L, 8);
    }

    @Test
    void supportsMaximumLimitAndRejectsUnboundedRequests() {
        when(doctorRepository.findByEmailIgnoreCase("a@example.com"))
                .thenReturn(Optional.of(doctor(4L, true)));
        when(activityService.getRecentActivities(4L, 20)).thenReturn(List.of());

        controller.getActivity(jwt("a@example.com"), 20);
        assertThrows(ResponseStatusException.class,
                () -> controller.getActivity(jwt("a@example.com"), 21));
        assertThrows(ResponseStatusException.class,
                () -> controller.getActivity(jwt("a@example.com"), 0));
        verify(activityService).getRecentActivities(4L, 20);
        verify(activityService, never()).getRecentActivities(4L, 21);
    }

    @Test
    void refusesMissingOrDisabledDoctorAndDoesNotQueryActivity() {
        assertThrows(ResponseStatusException.class, () -> controller.getActivity(null, 8));
        when(doctorRepository.findByEmailIgnoreCase("disabled@example.com"))
                .thenReturn(Optional.of(doctor(5L, false)));
        assertThrows(ResponseStatusException.class,
                () -> controller.getActivity(jwt("disabled@example.com"), 8));
        verify(activityService, never()).getRecentActivities(5L, 8);
    }

    private static Doctor doctor(Long id, boolean enabled) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setEnabled(enabled);
        return doctor;
    }

    private static Jwt jwt(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
