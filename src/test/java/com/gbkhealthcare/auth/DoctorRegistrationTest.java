package com.gbkhealthcare.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DoctorRegistrationTest {
    private DoctorRepository doctors;
    private PasswordEncoder passwordEncoder;
    private DoctorAuthService service;
    private Validator validator;

    @BeforeEach
    void setUp() {
        doctors = mock(DoctorRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new DoctorAuthService(doctors, passwordEncoder, mock(JwtTokenService.class),
                mock(RefreshSessionManager.class), 900);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void registrationNormalizesAndSavesProfileHashedAndPending() {
        DoctorRegistrationRequest request = request("  Dr. Example  ", "  DOCTOR@EXAMPLE.TEST ",
                "9876543210", " MBBS ", " Cardiology ", "  REG-123  ", "long-test-password");
        when(doctors.existsByEmailIgnoreCase("doctor@example.test")).thenReturn(false);
        when(passwordEncoder.encode("long-test-password")).thenReturn("bcrypt-hash");
        when(doctors.save(any(Doctor.class))).thenAnswer(call -> {
            Doctor saved = call.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        DoctorAuthService.RegistrationResult result = service.register(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Doctor.class);
        verify(doctors).save(captor.capture());
        Doctor saved = captor.getValue();
        assertEquals("Dr. Example", saved.getFullName());
        assertEquals("doctor@example.test", saved.getEmail());
        assertEquals("9876543210", saved.getMobileNumber());
        assertEquals("MBBS", saved.getQualification());
        assertEquals("Cardiology", saved.getSpecialization());
        assertEquals("REG-123", saved.getMedicalRegistrationNumber());
        assertEquals("bcrypt-hash", saved.getPasswordHash());
        assertTrue(saved.isEnabled());
        assertEquals(Doctor.ApprovalStatus.PENDING, saved.getApprovalStatus());
        assertEquals("PENDING", result.approvalStatus());
        verify(passwordEncoder).encode("long-test-password");
    }

    @Test
    void optionalMedicalRegistrationNumberMayBeMissingOrBlank() {
        DoctorRegistrationRequest missing = request("Doctor", "doctor@example.test", "9876543210",
                "MBBS", "General", null, "long-test-password");
        DoctorRegistrationRequest blank = request("Doctor", "doctor@example.test", "9876543210",
                "MBBS", "General", "  ", "long-test-password");
        assertTrue(validator.validate(missing).isEmpty());
        assertTrue(validator.validate(blank).isEmpty());

        when(doctors.existsByEmailIgnoreCase("doctor@example.test")).thenReturn(false);
        when(passwordEncoder.encode("long-test-password")).thenReturn("bcrypt-hash");
        when(doctors.save(any(Doctor.class))).thenAnswer(call -> call.getArgument(0));
        service.register(blank);
        var captor = org.mockito.ArgumentCaptor.forClass(Doctor.class);
        verify(doctors).save(captor.capture());
        assertNull(captor.getValue().getMedicalRegistrationNumber());
    }

    @Test
    void invalidMobileNumbersAreRejectedByBeanValidation() {
        for (String mobile : new String[]{null, "", "1234567890", "5876543210", "987654321", "+919876543210"}) {
            assertTrue(validator.validate(request("Doctor", "doctor@example.test", mobile,
                            "MBBS", "General", null, "long-test-password"))
                    .stream().anyMatch(error -> error.getPropertyPath().toString().equals("mobileNumber")),
                    "Expected mobile validation failure for: " + mobile);
        }
    }

    @Test
    void duplicateEmailIsConflictAndDoesNotHashOrSave() {
        when(doctors.existsByEmailIgnoreCase("doctor@example.test")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.register(request("Doctor", " DOCTOR@example.test ", "9876543210",
                        "MBBS", "General", null, "long-test-password")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(doctors, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void publicRegistrationRequestHasNoApprovalStatusField() {
        Set<String> fields = java.util.Arrays.stream(DoctorRegistrationRequest.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());
        assertFalse(fields.contains("approvalStatus"));
    }

    private static DoctorRegistrationRequest request(String name, String email, String mobile,
            String qualification, String specialization, String registration, String password) {
        return new DoctorRegistrationRequest(name, email, mobile, qualification, specialization, registration, password);
    }
}
