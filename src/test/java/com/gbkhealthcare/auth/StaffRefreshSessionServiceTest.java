package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentCaptor.forClass;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffRefreshSessionServiceTest {
    private final StaffRefreshSessionRepository sessions = mock(StaffRefreshSessionRepository.class);
    private final StaffAccountRepository accounts = mock(StaffAccountRepository.class);
    private final StaffRoleAssignmentRepository roles = mock(StaffRoleAssignmentRepository.class);
    private final JwtTokenService jwt = mock(JwtTokenService.class);
    private final StaffRefreshSessionService service = new StaffRefreshSessionService(sessions, accounts, roles, jwt, 3600);
    @Test void rotateRejectsAndRevokesFamilyWhenAllRolesWereRevoked() {
        StaffRefreshSession current = new StaffRefreshSession("family-a", 9L, "stored-hash", Instant.now().minusSeconds(2), Instant.now().plusSeconds(600));
        StaffAccount account = new StaffAccount(); account.setEmail("staff@example.test"); account.setEnabled(true);
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(accounts.findById(9L)).thenReturn(Optional.of(account));
        when(roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenReturn(List.of());
        when(sessions.findAllByFamilyId("family-a")).thenReturn(List.of(current));
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.rotate("previous-refresh-token"));
        assertEquals(401, error.getStatusCode().value()); assertNotNull(current.getRevokedAt());
        verify(sessions).saveAll(List.of(current)); verifyNoInteractions(jwt);
    }
    @Test void disabledStaffCannotRefresh() {
        StaffRefreshSession current = new StaffRefreshSession("family-b", 10L, "stored-hash", Instant.now().minusSeconds(2), Instant.now().plusSeconds(600));
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(accounts.findById(10L)).thenReturn(Optional.empty());
        when(sessions.findAllByFamilyId("family-b")).thenReturn(List.of(current));
        assertThrows(ResponseStatusException.class, () -> service.rotate("previous-refresh-token"));
        assertNotNull(current.getRevokedAt()); verify(sessions).saveAll(List.of(current));
    }
    @Test void replayedRefreshTokenRevokesItsSessionFamily() {
        StaffRefreshSession replayed = new StaffRefreshSession("family-replay", 11L, "stored-hash", Instant.now().minusSeconds(3), Instant.now().plusSeconds(600));
        replayed.revoke(Instant.now().minusSeconds(1));
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(replayed));
        when(sessions.findAllByFamilyId("family-replay")).thenReturn(List.of(replayed));
        assertThrows(ResponseStatusException.class, () -> service.rotate("replayed-token"));
        verify(sessions).saveAll(List.of(replayed)); verifyNoInteractions(accounts, roles, jwt);
    }
    @Test void refreshRotatesTokenAndStoresOnlyItsSha256Hash() throws Exception {
        StaffRefreshSession current = new StaffRefreshSession("family-rotate", 21L, "old-hash", Instant.now().minusSeconds(2), Instant.now().plusSeconds(600));
        StaffAccount account = new StaffAccount(); account.setId(21L); account.setEmail("nurse@example.test"); account.setEnabled(true);
        StaffRoleAssignment assignment = new StaffRoleAssignment(account, StaffRole.NURSE, 4L);
        when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(accounts.findById(21L)).thenReturn(Optional.of(account));
        when(roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(21L)).thenReturn(List.of(assignment));
        when(jwt.generateStaffToken(account)).thenReturn("rotated-access-token");
        StaffRefreshSessionService.IssuedSession result = service.rotate("old-raw-refresh-token");
        assertEquals("rotated-access-token", result.accessToken()); assertEquals("nurse@example.test", result.email());
        assertNotEquals("old-raw-refresh-token", result.refreshToken()); assertNotNull(current.getRevokedAt());
        ArgumentCaptor<StaffRefreshSession> saved = forClass(StaffRefreshSession.class);
        verify(sessions).save(saved.capture());
        String expectedHash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(result.refreshToken().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertEquals(expectedHash, saved.getValue().getTokenHash()); assertNotEquals(result.refreshToken(), saved.getValue().getTokenHash());
    }
}
