package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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

class PatientControllerPaginationTest {

    private PatientService patientService;
    private DoctorRepository doctorRepository;
    private PatientController controller;
    private Doctor doctor;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        patientService = mock(PatientService.class);
        doctorRepository = mock(DoctorRepository.class);
        controller = new PatientController(patientService, doctorRepository);
        doctor = new Doctor();
        doctor.setId(7L);
        doctor.setEmail("doctor@example.com");
        doctor.setEnabled(true);
        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("doctor@example.com")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(60))
                .build();
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.com"))
                .thenReturn(Optional.of(doctor));
    }

    @Test
    void pagedRouteScopesCombinedFiltersToAuthenticatedDoctor() {
        PatientPageResponse expected = new PatientPageResponse(
                List.of(), 0, 0, 10, 0, true, true);
        when(patientService.findPageByDoctorAccountId(7L, 0, 10, "Sam", "Female"))
                .thenReturn(expected);

        PatientPageResponse result = controller.findPage(jwt, 0, 10, "Sam", "Female");

        assertEquals(expected, result);
        verify(patientService).findPageByDoctorAccountId(7L, 0, 10, "Sam", "Female");
    }

    @Test
    void rejectsInvalidPageAndUnsupportedPageSize() {
        ResponseStatusException negativePage = assertThrows(ResponseStatusException.class,
                () -> controller.findPage(jwt, -1, 10, null, null));
        ResponseStatusException unsupportedSize = assertThrows(ResponseStatusException.class,
                () -> controller.findPage(jwt, 0, 15, null, null));

        assertEquals(HttpStatus.BAD_REQUEST, negativePage.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, unsupportedSize.getStatusCode());
        verify(patientService, never()).findPageByDoctorAccountId(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
