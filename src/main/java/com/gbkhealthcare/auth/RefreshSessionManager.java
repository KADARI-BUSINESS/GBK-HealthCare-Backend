package com.gbkhealthcare.auth;

import java.time.Instant;

public interface RefreshSessionManager {
    IssuedSession create(Long doctorId);
    IssuedSession rotate(String rawToken);
    void revoke(String rawToken);

    record IssuedSession(String refreshToken, String accessToken, Instant expiresAt, String doctorEmail) {}
}
