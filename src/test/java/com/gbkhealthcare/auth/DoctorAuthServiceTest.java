package com.gbkhealthcare.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorAuthServiceTest {

    private DoctorRepository doctorRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private RefreshSessionManager refreshSessionService;
    private DoctorAuthService service;

    @BeforeEach
    void setUp() {
        doctorRepository = mock(DoctorRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenService = mock(JwtTokenService.class);
        refreshSessionService = mock(RefreshSessionManager.class);
        service = new DoctorAuthService(doctorRepository, passwordEncoder, jwtTokenService, refreshSessionService, 3600);
    }

    @Test
    void successfulLoginReturnsBearerTokenResponse() {
        Doctor doctor = enabledDoctor();
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.com"))
                .thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("test-password", "encoded-hash"))
                .thenReturn(true);
        when(jwtTokenService.generateToken(doctor)).thenReturn("test-token");
        when(refreshSessionService.create(7L)).thenReturn(new RefreshSessionManager.IssuedSession(
                "opaque-refresh-secret", null, java.time.Instant.now().plusSeconds(3600), null));

        DoctorAuthService.LoginResult result = service.login(
                new DoctorLoginRequest(" doctor@example.com ", "test-password"), "test-correlation");

        assertEquals("test-token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(3600, result.expiresInSeconds());
        assertEquals("opaque-refresh-secret", result.refreshToken());
        verify(jwtTokenService).generateToken(doctor);
    }

    @Test
    void wrongPasswordIsRejectedWithoutGeneratingToken() {
        Doctor doctor = enabledDoctor();
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.com"))
                .thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("wrong-password", "encoded-hash"))
                .thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> service.login(
                new DoctorLoginRequest("doctor@example.com", "wrong-password"), "test-correlation"));
    }

    @Test
    void missingDoctorIsRejected() {
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> service.login(
                new DoctorLoginRequest("doctor@example.com", "test-password"), "test-correlation"));
    }

    @Test
    void disabledDoctorIsRejected() {
        Doctor doctor = enabledDoctor();
        doctor.setEnabled(false);
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.com"))
                .thenReturn(Optional.of(doctor));

        assertThrows(BadCredentialsException.class, () -> service.login(
                new DoctorLoginRequest("doctor@example.com", "test-password"), "test-correlation"));
    }

    private static Doctor enabledDoctor() {
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setEmail("doctor@example.com");
        doctor.setPasswordHash("encoded-hash");
        doctor.setEnabled(true);
        return doctor;
    }
}
