package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAuthServiceTest {
    private final StaffAccountRepository accounts = mock(StaffAccountRepository.class);
    private final StaffRoleAssignmentRepository roles = mock(StaffRoleAssignmentRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtTokenService jwt = mock(JwtTokenService.class);
    private final StaffRefreshSessionService sessions = mock(StaffRefreshSessionService.class);
    private final StaffAuthService service = new StaffAuthService(accounts, roles, passwords, jwt, sessions, 900);

    @Test void loginIssuesStaffOnlySessionForMultipleAssignedRoles() {
        StaffAccount account = staff("nurse@example.test", true); account.setId(41L);
        when(accounts.findByEmailIgnoreCase("nurse@example.test")).thenReturn(Optional.of(account));
        when(passwords.matches("a-long-password", "hash")).thenReturn(true);
        StaffAccount owner = staff("nurse@example.test", true);
        StaffRoleAssignment nurse = new StaffRoleAssignment(owner, StaffRole.NURSE, 2L);
        StaffRoleAssignment reception = new StaffRoleAssignment(owner, StaffRole.RECEPTION, 2L);
        when(roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(41L)).thenReturn(List.of(nurse, reception));
        when(jwt.generateStaffToken(account)).thenReturn("staff-access-token");
        when(sessions.create(41L)).thenReturn(new StaffRefreshSessionService.IssuedSession("raw-refresh", null, java.time.Instant.now().plusSeconds(500), null));
        StaffAuthService.LoginResult result = service.login(new StaffLoginRequest(" Nurse@Example.Test ", "a-long-password"));
        assertEquals("staff-access-token", result.accessToken());
        assertTrue(result.roles().containsAll(java.util.Set.of(StaffRole.NURSE, StaffRole.RECEPTION)));
        verify(jwt).generateStaffToken(account);
        verify(jwt, never()).generateAdminToken(any());
        verify(jwt, never()).generateToken(any(Doctor.class));
    }

    @Test void disabledAccountsAndAccountsWithoutRolesCannotLogin() {
        StaffAccount disabled = staff("disabled@example.test", false);
        when(accounts.findByEmailIgnoreCase("disabled@example.test")).thenReturn(Optional.of(disabled));
        assertThrows(BadCredentialsException.class, () -> service.login(new StaffLoginRequest("disabled@example.test", "password")));
        verifyNoInteractions(passwords, sessions, jwt);
        StaffAccount noRole = staff("norole@example.test", true); noRole.setId(5L);
        when(accounts.findByEmailIgnoreCase("norole@example.test")).thenReturn(Optional.of(noRole));
        when(passwords.matches("password", "hash")).thenReturn(true);
        when(roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(5L)).thenReturn(List.of());
        assertThrows(BadCredentialsException.class, () -> service.login(new StaffLoginRequest("norole@example.test", "password")));
        verify(sessions, never()).create(any());
    }
    private static StaffAccount staff(String email, boolean enabled) {
        StaffAccount account = new StaffAccount(); account.setEmail(email); account.setFullName("Test Staff"); account.setPasswordHash("hash"); account.setEnabled(enabled); return account;
    }
}
