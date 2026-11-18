-- Phase 1 Admin authentication schema. Review and apply through the approved
-- database change process; this script is never executed by the application.
CREATE TABLE admin_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_accounts_email UNIQUE (email)
);

CREATE TABLE admin_refresh_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_admin_refresh_token_hash UNIQUE (token_hash),
    INDEX idx_admin_refresh_admin_id (admin_id),
    INDEX idx_admin_refresh_family (family_id),
    CONSTRAINT fk_admin_refresh_admin FOREIGN KEY (admin_id) REFERENCES admin_accounts(id)
);
