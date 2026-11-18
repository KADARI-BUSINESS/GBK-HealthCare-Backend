package com.gbkhealthcare.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.Instant;

@Entity
@Table(name = "staff_role_assignments", uniqueConstraints = @UniqueConstraint(name = "uk_staff_role", columnNames = {"staff_id", "role_code"}), indexes = @Index(name = "idx_staff_role_active", columnList = "role_code, active, staff_id"))
public class StaffRoleAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffAccount staff;
    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 24)
    private StaffRole role;
    @JdbcTypeCode(Types.BIT)
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "assigned_by_admin_id", nullable = false)
    private Long assignedByAdminId;
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected StaffRoleAssignment() {}
    public StaffRoleAssignment(StaffAccount staff, StaffRole role, Long adminId) {
        this.staff = staff; this.role = role; this.assignedByAdminId = adminId; this.active = true; this.assignedAt = Instant.now();
    }
    public Long getId() { return id; }
    public StaffAccount getStaff() { return staff; }
    public StaffRole getRole() { return role; }
    public boolean isActive() { return active; }
    public Long getAssignedByAdminId() { return assignedByAdminId; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void assign(Long adminId) { active = true; assignedByAdminId = adminId; assignedAt = Instant.now(); revokedAt = null; }
    public void revoke() { active = false; revokedAt = Instant.now(); }
}
