package com.gbkhealthcare.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "staff_refresh_sessions", indexes = {
        @Index(name = "uk_staff_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_staff_refresh_account", columnList = "staff_id"),
        @Index(name = "idx_staff_refresh_family", columnList = "family_id")
})
public class StaffRefreshSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "staff_id", nullable = false)
    private Long staffId;
    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    protected StaffRefreshSession() {}
    public StaffRefreshSession(String familyId, Long staffId, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.familyId = familyId; this.staffId = staffId; this.tokenHash = tokenHash; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    public Long getStaffId() { return staffId; }
    public String getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void revoke(Instant at) { if (revokedAt == null) revokedAt = at; }
}
