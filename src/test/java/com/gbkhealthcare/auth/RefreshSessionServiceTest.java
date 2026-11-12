package com.gbkhealthcare.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RefreshSessionServiceTest {
    private DoctorRefreshSessionRepository sessions;
    private DoctorRepository doctors;
    private JwtTokenService jwtTokens;
    private RefreshSessionService service;

    @BeforeEach
    void setUp() {
        sessions = mock(DoctorRefreshSessionRepository.class);
        doctors = mock(DoctorRepository.class);
        jwtTokens = mock(JwtTokenService.class);
        service = new RefreshSessionService(sessions, doctors, jwtTokens, 3600);
    }

    @Test
    void createReturnsOpaqueTokenAndPersistsOnlyItsHash() {
        var issued = service.create(7L);
        assertNotNull(issued.refreshToken());
        assertTrue(issued.refreshToken().length() >= 40);
        var captor = org.mockito.ArgumentCaptor.forClass(DoctorRefreshSession.class);
        verify(sessions).save(captor.capture());
        assertNotEquals(issued.refreshToken(), captor.getValue().getTokenHash());
        assertEquals(64, captor.getValue().getTokenHash().length());
    }

    @Test
    void rotationRevokesOldTokenAndReturnsNewAccessAndRefreshTokens() throws Exception {
        String rawToken = "valid-refresh-token";
        DoctorRefreshSession current = session(rawToken, Instant.now().plusSeconds(600), null);
        Doctor doctor = enabledDoctor();
        when(sessions.findByTokenHashForUpdate(hash(rawToken))).thenReturn(Optional.of(current));
        when(doctors.findById(7L)).thenReturn(Optional.of(doctor));
        when(jwtTokens.generateToken(doctor)).thenReturn("short-access-token");

        var rotated = service.rotate(rawToken);

        assertNotEquals(rawToken, rotated.refreshToken());
        assertEquals("short-access-token", rotated.accessToken());
        assertEquals("doctor@example.com", rotated.doctorEmail());
        assertNotNull(current.getRevokedAt());
        verify(sessions).save(any(DoctorRefreshSession.class));
    }

    @Test
    void reusingRevokedTokenInvalidatesItsSessionFamily() throws Exception {
        String rawToken = "replayed-refresh-token";
        DoctorRefreshSession revoked = session(rawToken, Instant.now().plusSeconds(600), Instant.now());
        when(sessions.findByTokenHashForUpdate(hash(rawToken))).thenReturn(Optional.of(revoked));
        when(sessions.findAllByFamilyId("family-1")).thenReturn(List.of(revoked));

        assertThrows(ResponseStatusException.class, () -> service.rotate(rawToken));
        verify(sessions).saveAll(List.of(revoked));
    }

    private static DoctorRefreshSession session(String token, Instant expires, Instant revoked) throws Exception {
        DoctorRefreshSession session = new DoctorRefreshSession("family-1", 7L, hash(token), Instant.now(), expires);
        if (revoked != null) session.revoke(revoked);
        return session;
    }

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
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
