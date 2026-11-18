-- Review-only Phase 1 staff authentication migration.
-- Requires the existing admin_accounts table. Do not execute until separately approved.
USE `gbk_healthcare_opd_test`;
CREATE TABLE staff_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    mobile_number VARCHAR(10) NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_staff_accounts_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE staff_role_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    role_code VARCHAR(24) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    assigned_by_admin_id BIGINT NOT NULL,
    assigned_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_staff_role UNIQUE (staff_id, role_code),
    INDEX idx_staff_role_active (role_code, active, staff_id),
    CONSTRAINT fk_staff_role_account FOREIGN KEY (staff_id) REFERENCES staff_accounts(id),
    CONSTRAINT fk_staff_role_admin FOREIGN KEY (assigned_by_admin_id) REFERENCES admin_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE staff_refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    staff_id BIGINT NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_staff_refresh_token_hash UNIQUE (token_hash),
    INDEX idx_staff_refresh_account (staff_id),
    INDEX idx_staff_refresh_family (family_id),
    CONSTRAINT fk_staff_refresh_account FOREIGN KEY (staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE staff_account_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_admin_id BIGINT NOT NULL,
    actor_admin_email VARCHAR(160) NOT NULL,
    actor_role VARCHAR(24) NOT NULL,
    target_staff_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    detail VARCHAR(300) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_staff_audit_target_time (target_staff_id, occurred_at, id),
    INDEX idx_staff_audit_actor_time (actor_admin_id, occurred_at, id),
    CONSTRAINT fk_staff_audit_admin FOREIGN KEY (actor_admin_id) REFERENCES admin_accounts(id),
    CONSTRAINT fk_staff_audit_target FOREIGN KEY (target_staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
