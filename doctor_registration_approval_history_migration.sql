-- Review-only migration for Doctor registration profile fields and approval audit history.
-- Do not execute until the schema change is separately approved.

ALTER TABLE doctors
    ADD COLUMN mobile_number VARCHAR(10) NULL,
    ADD COLUMN qualification VARCHAR(120) NULL,
    ADD COLUMN specialization VARCHAR(120) NULL,
    ADD COLUMN medical_registration_number VARCHAR(100) NULL;

CREATE TABLE doctor_approval_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    previous_approval_status VARCHAR(20) NOT NULL,
    new_approval_status VARCHAR(20) NOT NULL,
    admin_id BIGINT NOT NULL,
    admin_email VARCHAR(160) NOT NULL,
    decision_at DATETIME(6) NOT NULL,
    rejection_reason VARCHAR(500) NULL,
    PRIMARY KEY (id),
    INDEX idx_doctor_approval_history_decision (decision_at, id),
    INDEX idx_doctor_approval_history_doctor (doctor_id, decision_at, id),
    CONSTRAINT fk_doctor_approval_history_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_doctor_approval_history_admin
        FOREIGN KEY (admin_id) REFERENCES admin_accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
