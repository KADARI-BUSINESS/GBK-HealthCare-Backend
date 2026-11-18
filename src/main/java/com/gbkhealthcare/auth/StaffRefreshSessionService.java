package com.gbkhealthcare.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class StaffRefreshSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StaffRefreshSessionRepository sessions;
    private final StaffAccountRepository accounts;
    private final StaffRoleAssignmentRepository roles;
    private final JwtTokenService jwtTokens;
    private final long lifetimeSeconds;

    public StaffRefreshSessionService(StaffRefreshSessionRepository sessions, StaffAccountRepository accounts,
            StaffRoleAssignmentRepository roles, JwtTokenService jwtTokens,
            @Value("${app.security.staff-refresh-expiration-seconds:2592000}") long lifetimeSeconds) {
        if (lifetimeSeconds <= 0) throw new IllegalArgumentException("Staff refresh expiration must be positive.");
        this.sessions = sessions; this.accounts = accounts; this.roles = roles; this.jwtTokens = jwtTokens;
        this.lifetimeSeconds = lifetimeSeconds;
    }

    @Transactional
    public IssuedSession create(Long staffId) { return issue(UUID.randomUUID().toString(), staffId); }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public IssuedSession rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalidSession();
        StaffRefreshSession current = sessions.findByTokenHashForUpdate(hash(rawToken)).orElseThrow(StaffRefreshSessionService::invalidSession);
        if (current.getRevokedAt() != null) { revokeFamily(current.getFamilyId()); throw invalidSession(); }
        Instant now = Instant.now();
        if (!current.getExpiresAt().isAfter(now)) { current.revoke(now); throw invalidSession(); }
        StaffAccount staff = accounts.findById(current.getStaffId()).filter(StaffAccount::isEnabled)
                .orElseThrow(() -> { revokeFamily(current.getFamilyId()); return invalidSession(); });
        if (roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(staff.getId()).isEmpty()) {
            revokeFamily(current.getFamilyId()); throw invalidSession();
        }
        current.revoke(now);
        IssuedSession next = issue(current.getFamilyId(), staff.getId());
        return new IssuedSession(next.refreshToken(), jwtTokens.generateStaffToken(staff), next.expiresAt(), staff.getEmail());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHashForUpdate(hash(rawToken)).ifPresent(s -> revokeFamily(s.getFamilyId()));
    }

    @Transactional
    public void revokeAll(Long staffId) {
        List<StaffRefreshSession> active = sessions.findAllByStaffIdAndRevokedAtIsNull(staffId);
        Instant now = Instant.now(); active.forEach(s -> s.revoke(now)); sessions.saveAll(active);
    }

    private IssuedSession issue(String family, Long staffId) {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now(), expiry = now.plusSeconds(lifetimeSeconds);
        sessions.save(new StaffRefreshSession(family, staffId, hash(raw), now, expiry));
        return new IssuedSession(raw, null, expiry, null);
    }

    private void revokeFamily(String family) {
        List<StaffRefreshSession> found = sessions.findAllByFamilyId(family);
        Instant now = Instant.now(); found.forEach(s -> s.revoke(now)); sessions.saveAll(found);
    }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("SHA-256 is unavailable.", exception); }
    }
    private static ResponseStatusException invalidSession() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff refresh session is invalid or expired."); }
    public record IssuedSession(String refreshToken, String accessToken, Instant expiresAt, String email) {}
}
