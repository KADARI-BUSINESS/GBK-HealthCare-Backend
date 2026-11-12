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
public class RefreshSessionService implements RefreshSessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DoctorRefreshSessionRepository sessions;
    private final DoctorRepository doctors;
    private final JwtTokenService jwtTokens;
    private final long lifetimeSeconds;

    public RefreshSessionService(DoctorRefreshSessionRepository sessions, DoctorRepository doctors,
            JwtTokenService jwtTokens, @Value("${app.security.refresh-expiration-seconds:2592000}") long lifetimeSeconds) {
        if (lifetimeSeconds <= 0) throw new IllegalArgumentException("Refresh expiration must be positive.");
        this.sessions = sessions;
        this.doctors = doctors;
        this.jwtTokens = jwtTokens;
        this.lifetimeSeconds = lifetimeSeconds;
    }

    @Transactional
    @Override
    public RefreshSessionManager.IssuedSession create(Long doctorId) {
        return create(UUID.randomUUID().toString(), doctorId);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    @Override
    public RefreshSessionManager.IssuedSession rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalidSession();
        String hash = hash(rawToken);
        DoctorRefreshSession current = sessions.findByTokenHashForUpdate(hash).orElseThrow(RefreshSessionService::invalidSession);
        if (current.getRevokedAt() != null) {
            revokeFamily(current.getFamilyId());
            throw invalidSession();
        }
        Instant now = Instant.now();
        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(now);
            throw invalidSession();
        }
        Doctor doctor = doctors.findById(current.getDoctorId()).filter(Doctor::isEnabled).orElseThrow(() -> {
            revokeFamily(current.getFamilyId());
            return invalidSession();
        });
        current.revoke(now);
        IssuedSession next = create(current.getFamilyId(), doctor.getId());
        return new RefreshSessionManager.IssuedSession(next.refreshToken(), jwtTokens.generateToken(doctor), next.expiresAt(), doctor.getEmail());
    }

    @Transactional
    @Override
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessions.findByTokenHashForUpdate(hash(rawToken)).ifPresent(session -> revokeFamily(session.getFamilyId()));
    }

    private RefreshSessionManager.IssuedSession create(String familyId, Long doctorId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(lifetimeSeconds);
        sessions.save(new DoctorRefreshSession(familyId, doctorId, hash(token), now, expiry));
        return new RefreshSessionManager.IssuedSession(token, null, expiry, null);
    }

    private void revokeFamily(String familyId) {
        List<DoctorRefreshSession> family = sessions.findAllByFamilyId(familyId);
        family.forEach(session -> session.revoke(Instant.now()));
        sessions.saveAll(family);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private static ResponseStatusException invalidSession() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session is invalid or expired.");
    }

}
