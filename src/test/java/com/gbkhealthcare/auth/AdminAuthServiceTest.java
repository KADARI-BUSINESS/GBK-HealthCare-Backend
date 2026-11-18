package com.gbkhealthcare.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AdminAuthServiceTest {
    private AdminAccountRepository admins;
    private JwtTokenService jwt;
    private AdminRefreshSessionService sessions;
    private AdminAuthService service;

    @BeforeEach void setUp() {
        admins = mock(AdminAccountRepository.class); jwt = mock(JwtTokenService.class);
        sessions = mock(AdminRefreshSessionService.class);
        service = new AdminAuthService(admins, new BCryptPasswordEncoder(4), jwt, sessions, 900);
    }

    @Test void successfulLoginChecksBCryptAndIssuesAdminTokenAndSeparateSession() {
        AdminAccount account = admin();
        account.setPasswordHash(new BCryptPasswordEncoder(4).encode("strong-test-password"));
        when(admins.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(account));
        when(jwt.generateAdminToken(account)).thenReturn("admin-jwt");
        var issued = new AdminRefreshSessionService.IssuedSession("admin-refresh", null, Instant.now().plusSeconds(60), null);
        when(sessions.create(11L)).thenReturn(issued);

        var result = service.login(new AdminLoginRequest(" Admin@example.com ", "strong-test-password"));

        assertEquals("admin-jwt", result.accessToken());
        assertEquals("admin@example.com", result.adminEmail());
        assertEquals("admin-refresh", result.session().refreshToken());
        verify(jwt).generateAdminToken(account);
        verify(sessions).create(11L);
    }

    @Test void incorrectPasswordIsRejectedWithoutSession() {
        AdminAccount account = admin();
        account.setPasswordHash(new BCryptPasswordEncoder(4).encode("right-password"));
        when(admins.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(account));
        assertThrows(BadCredentialsException.class, () -> service.login(new AdminLoginRequest("admin@example.com", "wrong-password")));
        verifyNoInteractions(sessions);
        verifyNoInteractions(jwt);
    }

    @Test void disabledOrMissingAdminIsRejected() {
        AdminAccount account = admin(); account.setEnabled(false);
        when(admins.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(account));
        assertThrows(BadCredentialsException.class, () -> service.login(new AdminLoginRequest("admin@example.com", "password")));
        verifyNoInteractions(sessions);
    }

    private static AdminAccount admin() {
        AdminAccount account = new AdminAccount(); account.setId(11L); account.setEmail("admin@example.com");
        account.setEnabled(true); return account;
    }
}
