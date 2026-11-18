-- REVIEW ONLY. Do not execute until staff_auth_migration.sql, reception_opd_migration.sql,
-- and nursing_intake_migration.sql have been reviewed and applied to an isolated schema.
-- Additive only: preserves Patient and PatientVisit identities, all existing rows, and legacy ownership.

ALTER TABLE doctor_queue_entries
    ADD COLUMN claimed_at DATETIME(6) NULL,
    ADD COLUMN consultation_started_at DATETIME(6) NULL,
    ADD COLUMN held_at DATETIME(6) NULL,
    ADD COLUMN signed_at DATETIME(6) NULL;

ALTER TABLE patient_visits
    ADD COLUMN encounter_status VARCHAR(32) NULL,
    ADD COLUMN consultation_status VARCHAR(16) NULL,
    ADD COLUMN assessment TEXT NULL,
    ADD COLUMN doctor_notes TEXT NULL,
    ADD COLUMN consultation_signed_at DATETIME(6) NULL,
    ADD COLUMN checkout_status VARCHAR(24) NULL;

CREATE TABLE consultation_addenda (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_visit_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    addendum_text TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_consult_addenda_visit_created (patient_visit_id, created_at),
    CONSTRAINT fk_consult_addenda_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits (id),
    CONSTRAINT fk_consult_addenda_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id)
) ENGINE=InnoDB;

CREATE TABLE patient_history_access_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    encounter_id BIGINT NOT NULL,
    accessed_resource VARCHAR(48) NOT NULL,
    accessed_resource_id BIGINT NULL,
    accessed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_history_access_doctor_time (doctor_id, accessed_at),
    INDEX idx_history_access_patient_time (patient_id, accessed_at),
    INDEX idx_history_access_encounter (encounter_id, accessed_at),
    CONSTRAINT fk_history_access_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_history_access_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_history_access_encounter FOREIGN KEY (encounter_id) REFERENCES patient_visits (id)
) ENGINE=InnoDB;

CREATE TABLE doctor_opd_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    patient_visit_id BIGINT NOT NULL,
    queue_entry_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_doctor_opd_audit_encounter_time (patient_visit_id, occurred_at),
    INDEX idx_doctor_opd_audit_actor_time (doctor_id, occurred_at),
    CONSTRAINT fk_doctor_opd_audit_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    CONSTRAINT fk_doctor_opd_audit_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits (id),
    CONSTRAINT fk_doctor_opd_audit_queue FOREIGN KEY (queue_entry_id) REFERENCES doctor_queue_entries (id)
) ENGINE=InnoDB;
