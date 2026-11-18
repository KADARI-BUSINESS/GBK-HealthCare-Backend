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

class AdminRefreshSessionServiceTest {
    private AdminRefreshSessionRepository sessions;
    private AdminAccountRepository admins;
    private JwtTokenService jwt;
    private AdminRefreshSessionService service;

    @BeforeEach void setUp() {
        sessions = mock(AdminRefreshSessionRepository.class); admins = mock(AdminAccountRepository.class); jwt = mock(JwtTokenService.class);
        service = new AdminRefreshSessionService(sessions, admins, jwt, 3600);
    }

    @Test void createPersistsOnlySha256TokenHash() throws Exception {
        var issued = service.create(11L);
        var captor = org.mockito.ArgumentCaptor.forClass(AdminRefreshSession.class);
        verify(sessions).save(captor.capture());
        assertNotNull(issued.refreshToken()); assertNotEquals(issued.refreshToken(), captor.getValue().getTokenHash());
        assertEquals(hash(issued.refreshToken()), captor.getValue().getTokenHash());
    }

    @Test void rotateRevokesCurrentTokenAndIssuesDistinctSessionAndAdminJwt() throws Exception {
        String raw = "admin-refresh-secret";
        AdminRefreshSession current = new AdminRefreshSession("family-a", 11L, hash(raw), Instant.now(), Instant.now().plusSeconds(600));
        AdminAccount admin = new AdminAccount(); admin.setId(11L); admin.setEmail("admin@example.com"); admin.setEnabled(true);
        when(sessions.findByTokenHashForUpdate(hash(raw))).thenReturn(Optional.of(current));
        when(admins.findById(11L)).thenReturn(Optional.of(admin)); when(jwt.generateAdminToken(admin)).thenReturn("admin-access");
        var rotated = service.rotate(raw);
        assertNotEquals(raw, rotated.refreshToken()); assertEquals("admin-access", rotated.accessToken());
        assertEquals("admin@example.com", rotated.adminEmail()); assertNotNull(current.getRevokedAt());
        verify(sessions).save(any(AdminRefreshSession.class));
    }

    @Test void replayedRevokedTokenRevokesItsAdminFamily() throws Exception {
        String raw = "replayed-admin-refresh";
        AdminRefreshSession revoked = new AdminRefreshSession("family-a", 11L, hash(raw), Instant.now(), Instant.now().plusSeconds(60));
        revoked.revoke(Instant.now());
        when(sessions.findByTokenHashForUpdate(hash(raw))).thenReturn(Optional.of(revoked));
        when(sessions.findAllByFamilyId("family-a")).thenReturn(List.of(revoked));
        assertThrows(ResponseStatusException.class, () -> service.rotate(raw));
        verify(sessions).saveAll(List.of(revoked));
    }

    private static String hash(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    }
}
