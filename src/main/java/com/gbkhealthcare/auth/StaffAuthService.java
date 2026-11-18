package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StaffAuthService {
    private final StaffAccountRepository accounts;
    private final StaffRoleAssignmentRepository roleAssignments;
    private final PasswordEncoder passwords;
    private final JwtTokenService jwtTokens;
    private final StaffRefreshSessionService sessions;
    private final long accessLifetime;

    public StaffAuthService(StaffAccountRepository accounts, StaffRoleAssignmentRepository roleAssignments,
            PasswordEncoder passwords, JwtTokenService jwtTokens, StaffRefreshSessionService sessions,
            @Value("${app.security.jwt-expiration-seconds:900}") long accessLifetime) {
        this.accounts = accounts; this.roleAssignments = roleAssignments; this.passwords = passwords;
        this.jwtTokens = jwtTokens; this.sessions = sessions; this.accessLifetime = accessLifetime;
    }

    @Transactional
    public LoginResult login(StaffLoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new BadCredentialsException("Invalid email or password.");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        StaffAccount account = accounts.findByEmailIgnoreCase(email).filter(StaffAccount::isEnabled)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));
        if (!passwords.matches(request.password(), account.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password.");
        Set<StaffRole> roles = activeRoles(account.getId());
        if (roles.isEmpty()) throw new BadCredentialsException("Invalid email or password.");
        return new LoginResult(jwtTokens.generateStaffToken(account), accessLifetime,
                account.getEmail(), roles, sessions.create(account.getId()));
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse profile(String email) {
        StaffAccount account = accounts.findByEmailIgnoreCase(email)
                .filter(StaffAccount::isEnabled)
                .orElseThrow(StaffAccessService::inactiveStaff);
        Set<StaffRole> roles = activeRoles(account.getId());
        if (roles.isEmpty()) throw StaffAccessService.inactiveStaff();
        return new StaffProfileResponse(account.getId(), account.getFullName(), account.getEmail(), account.getMobileNumber(), roles);
    }

    @Transactional(readOnly = true)
    Set<StaffRole> activeRoles(Long staffId) {
        return roleAssignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(staffId).stream()
                .map(StaffRoleAssignment::getRole).collect(Collectors.toUnmodifiableSet());
    }

    public long getAccessLifetime() { return accessLifetime; }
    public record LoginResult(String accessToken, long expiresInSeconds, String email, Set<StaffRole> roles,
            StaffRefreshSessionService.IssuedSession session) {}
}
