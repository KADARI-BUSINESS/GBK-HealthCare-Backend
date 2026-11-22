package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;

@Service
public class AdminAuthService {
    private final AdminAccountRepository admins;
    private final PasswordEncoder passwords;
    private final JwtTokenService jwtTokens;
    private final AdminRefreshSessionService sessions;
    private final long accessLifetime;

    public AdminAuthService(AdminAccountRepository admins, PasswordEncoder passwords, JwtTokenService jwtTokens,
            AdminRefreshSessionService sessions, @Value("${app.security.jwt-expiration-seconds:900}") long accessLifetime) {
        this.admins = admins; this.passwords = passwords; this.jwtTokens = jwtTokens; this.sessions = sessions; this.accessLifetime = accessLifetime;
    }

    public LoginResult login(AdminLoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        AdminAccount admin = admins.findByEmailIgnoreCase(email).filter(AdminAccount::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (!passwords.matches(request.password(), admin.getPasswordHash())) throw new BadCredentialsException("Invalid email or password.");
        return new LoginResult(jwtTokens.generateAdminToken(admin), accessLifetime, admin.getEmail(), sessions.create(admin.getId()));
    }
    public long getAccessLifetime() { return accessLifetime; }
    public record LoginResult(String accessToken, long expiresInSeconds, String adminEmail, AdminRefreshSessionService.IssuedSession session) {}
}
