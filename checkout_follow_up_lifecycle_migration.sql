-- REVIEW ONLY for Phase 7. Do not execute or apply without separate approval.
-- Additive only; preserves existing Patient, PatientVisit and PatientFollowUp rows.
-- Prerequisites: reviewed Staff, Reception/OPD, Doctor consultation and Billing schemas.
-- Confirm actual local columns, keys and table engines before any later migration approval.

ALTER TABLE patient_visits
  ADD COLUMN checkout_completed_at DATETIME(6) NULL,
  ADD COLUMN checkout_completed_by_staff_id BIGINT NULL,
  ADD CONSTRAINT fk_visit_checkout_completed_staff
    FOREIGN KEY (checkout_completed_by_staff_id) REFERENCES staff_accounts(id),
  ADD INDEX idx_patient_visits_checkout_complete (checkout_status, checkout_completed_at);

ALTER TABLE patient_follow_ups
  ADD COLUMN checked_in_at DATETIME(6) NULL,
  ADD COLUMN started_at DATETIME(6) NULL,
  ADD COLUMN cancelled_at DATETIME(6) NULL,
  ADD COLUMN cancelled_by_actor_type VARCHAR(16) NULL,
  ADD COLUMN cancelled_by_actor_id BIGINT NULL,
  ADD COLUMN cancellation_reason VARCHAR(1000) NULL,
  ADD COLUMN missed_at DATETIME(6) NULL,
  ADD COLUMN missed_by_actor_type VARCHAR(16) NULL,
  ADD COLUMN missed_by_actor_id BIGINT NULL,
  ADD COLUMN missed_reason VARCHAR(1000) NULL,
  ADD INDEX idx_patient_follow_ups_status_date (status, follow_up_date);

CREATE TABLE patient_follow_up_audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  follow_up_id BIGINT NOT NULL,
  previous_status VARCHAR(20) NULL,
  new_status VARCHAR(20) NOT NULL,
  action_type VARCHAR(32) NOT NULL,
  actor_type VARCHAR(16) NOT NULL,
  actor_id BIGINT NOT NULL,
  reason VARCHAR(1000) NULL,
  encounter_id BIGINT NULL,
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_follow_up_audit_follow_up_time (follow_up_id, occurred_at),
  INDEX idx_follow_up_audit_actor_time (actor_type, actor_id, occurred_at),
  CONSTRAINT fk_follow_up_audit_follow_up FOREIGN KEY (follow_up_id) REFERENCES patient_follow_ups(id),
  CONSTRAINT fk_follow_up_audit_encounter FOREIGN KEY (encounter_id) REFERENCES patient_visits(id)
) ENGINE=InnoDB;
