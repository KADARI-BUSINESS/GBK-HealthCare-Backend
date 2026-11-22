package com.gbkhealthcare.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "staff_account_audit_events", indexes = {
        @Index(name = "idx_staff_audit_target_time", columnList = "target_staff_id, occurred_at, id"),
        @Index(name = "idx_staff_audit_actor_time", columnList = "actor_admin_id, occurred_at, id")
})
public class StaffAccountAuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_admin_id", nullable = false)
    private Long actorAdminId;
    @Column(name = "actor_admin_email", nullable = false, length = 160)
    private String actorAdminEmail;
    @Column(name = "actor_role", nullable = false, length = 24)
    private String actorRole;
    @Column(name = "target_staff_id", nullable = false)
    private Long targetStaffId;
    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;
    @Column(name = "detail", nullable = false, length = 300)
    private String detail;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
    protected StaffAccountAuditEvent() {}
    public StaffAccountAuditEvent(Long adminId, String adminEmail, Long staffId, String action, String detail) {
        actorAdminId = adminId; actorAdminEmail = adminEmail; actorRole = "ADMIN"; targetStaffId = staffId; actionType = action; this.detail = detail; occurredAt = Instant.now();
    }
}
