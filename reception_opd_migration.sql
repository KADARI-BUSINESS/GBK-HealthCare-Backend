-- Review-only additive migration for Phase 2 Reception/OPD check-in.
-- Prerequisites: existing patients, patient_visits, patient_follow_ups, doctors,
-- and staff_accounts tables. Inspect the actual local schema before approval.
-- Do not execute this script until separately approved.
USE `gbk_healthcare_opd_test`;
ALTER TABLE patient_visits
    ADD COLUMN encounter_type VARCHAR(20) NULL,
    ADD COLUMN assigned_doctor_id BIGINT NULL,
    ADD COLUMN checked_in_by_staff_id BIGINT NULL,
    ADD COLUMN emergency_priority BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN emergency_note TEXT NULL,
    ADD COLUMN source_follow_up_id BIGINT NULL,
    ADD COLUMN check_in_request_key VARCHAR(36) NULL,
    ADD UNIQUE INDEX uk_patient_visits_follow_up (source_follow_up_id),
    ADD UNIQUE INDEX uk_patient_visits_check_in_key (check_in_request_key),
    ADD INDEX idx_patient_visits_assigned_doctor_date (assigned_doctor_id, visit_date),
    ADD CONSTRAINT fk_patient_visit_assigned_doctor
        FOREIGN KEY (assigned_doctor_id) REFERENCES doctors(id),
    ADD CONSTRAINT fk_patient_visit_checked_in_staff
        FOREIGN KEY (checked_in_by_staff_id) REFERENCES staff_accounts(id),
    ADD CONSTRAINT fk_patient_visit_source_follow_up
        FOREIGN KEY (source_follow_up_id) REFERENCES patient_follow_ups(id);

CREATE TABLE nursing_queue_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_visit_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'WAITING',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_nursing_queue_patient_visit UNIQUE (patient_visit_id),
    INDEX idx_nursing_queue_status_created (status, created_at),
    CONSTRAINT fk_nursing_queue_patient_visit
        FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
