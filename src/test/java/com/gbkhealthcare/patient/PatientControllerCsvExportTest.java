package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientControllerCsvExportTest {
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
        doctor.setId(77L);
        doctor.setEnabled(true);
        jwt = Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("doctor@example.test").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60)).build();
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.test"))
                .thenReturn(Optional.of(doctor));
    }

    @Test
    void setsDownloadHeadersAndStreamsOnlyAuthenticatedDoctorsExport() throws Exception {
        var response = controller.exportCsv(jwt, " Sam ", "Female");

        assertEquals(MediaType.parseMediaType("text/csv; charset=UTF-8"),
                response.getHeaders().getContentType());
        assertEquals("attachment; filename=\"gbk-healthcare-patients.csv\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        response.getBody().writeTo(output);
        verify(patientService).writeCsvForDoctor(77L, " Sam ", "Female", output);
    }

    @Test
    void refusesDisabledDoctor() {
        doctor.setEnabled(false);
        assertThrows(ResponseStatusException.class,
                () -> controller.exportCsv(jwt, null, null));
        verifyNoInteractions(patientService);
    }
}
