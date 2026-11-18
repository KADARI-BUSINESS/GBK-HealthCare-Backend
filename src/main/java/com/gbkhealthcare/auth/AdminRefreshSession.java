package com.gbkhealthcare.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_refresh_sessions", indexes = {
        @Index(name = "idx_admin_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_admin_refresh_admin_id", columnList = "admin_id"),
        @Index(name = "idx_admin_refresh_family", columnList = "family_id")
})
public class AdminRefreshSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "admin_id", nullable = false)
    private Long adminId;
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected AdminRefreshSession() {}
    public AdminRefreshSession(String familyId, Long adminId, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.familyId = familyId; this.adminId = adminId; this.tokenHash = tokenHash;
        this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    public Long getAdminId() { return adminId; }
    public String getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void revoke(Instant time) { if (revokedAt == null) revokedAt = time; }
}
