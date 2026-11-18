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
public class AdminRefreshSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AdminRefreshSessionRepository sessions;
    private final AdminAccountRepository admins;
    private final JwtTokenService jwtTokens;
    private final long lifetimeSeconds;

    public AdminRefreshSessionService(AdminRefreshSessionRepository sessions, AdminAccountRepository admins,
            JwtTokenService jwtTokens, @Value("${app.security.admin-refresh-expiration-seconds:2592000}") long lifetimeSeconds) {
        if (lifetimeSeconds <= 0) throw new IllegalArgumentException("Admin refresh expiration must be positive.");
        this.sessions = sessions; this.admins = admins; this.jwtTokens = jwtTokens; this.lifetimeSeconds = lifetimeSeconds;
    }

    @Transactional
    public IssuedSession create(Long adminId) { return create(UUID.randomUUID().toString(), adminId); }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public IssuedSession rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalidSession();
        AdminRefreshSession current = sessions.findByTokenHashForUpdate(hash(rawToken)).orElseThrow(AdminRefreshSessionService::invalidSession);
        if (current.getRevokedAt() != null) { revokeFamily(current.getFamilyId()); throw invalidSession(); }
        Instant now = Instant.now();
        if (!current.getExpiresAt().isAfter(now)) { current.revoke(now); throw invalidSession(); }
        AdminAccount admin = admins.findById(current.getAdminId()).filter(AdminAccount::isEnabled)
                .orElseThrow(() -> { revokeFamily(current.getFamilyId()); return invalidSession(); });
        current.revoke(now);
        IssuedSession next = create(current.getFamilyId(), admin.getId());
        return new IssuedSession(next.refreshToken(), jwtTokens.generateAdminToken(admin), next.expiresAt(), admin.getEmail());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHashForUpdate(hash(rawToken)).ifPresent(s -> revokeFamily(s.getFamilyId()));
    }

    private IssuedSession create(String family, Long adminId) {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now(), expiry = now.plusSeconds(lifetimeSeconds);
        sessions.save(new AdminRefreshSession(family, adminId, hash(token), now, expiry));
        return new IssuedSession(token, null, expiry, null);
    }

    private void revokeFamily(String familyId) {
        List<AdminRefreshSession> family = sessions.findAllByFamilyId(familyId);
        family.forEach(s -> s.revoke(Instant.now())); sessions.saveAll(family);
    }

    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException("SHA-256 is unavailable.", e); }
    }
    private static ResponseStatusException invalidSession() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin refresh session is invalid or expired."); }
    public record IssuedSession(String refreshToken, String accessToken, Instant expiresAt, String adminEmail) {}
}
