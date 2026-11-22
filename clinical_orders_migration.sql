-- REVIEW ONLY: do not execute until the target schema has been inspected and approved.
-- Adds encounter-scoped prescriptions, investigation orders, referrals and append-only audit events.
-- Requires the existing patient_visits and doctors tables (from the prior OPD schema).

CREATE TABLE prescriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_visit_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    revision INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    supersedes_prescription_id BIGINT NULL,
    notes TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    signed_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_prescription_visit_revision UNIQUE (patient_visit_id, revision),
    INDEX idx_prescription_visit_status (patient_visit_id, status),
    CONSTRAINT fk_prescription_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id),
    CONSTRAINT fk_prescription_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_prescription_supersedes FOREIGN KEY (supersedes_prescription_id) REFERENCES prescriptions(id)
) ENGINE=InnoDB;

CREATE TABLE prescription_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    item_order INT NOT NULL,
    medicine_name VARCHAR(180) NOT NULL,
    dosage VARCHAR(120) NOT NULL,
    route VARCHAR(80) NOT NULL,
    frequency VARCHAR(120) NOT NULL,
    duration VARCHAR(80) NOT NULL,
    quantity DECIMAL(7,2) NOT NULL,
    instructions TEXT NULL,
    start_date DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_prescription_item_order UNIQUE (prescription_id, item_order),
    CONSTRAINT fk_prescription_item_header FOREIGN KEY (prescription_id) REFERENCES prescriptions(id)
) ENGINE=InnoDB;

CREATE TABLE investigation_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_visit_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(180) NOT NULL,
    indication TEXT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ordered_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_investigation_visit_status (patient_visit_id, status),
    INDEX idx_investigation_doctor_time (doctor_id, ordered_at),
    CONSTRAINT fk_investigation_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id),
    CONSTRAINT fk_investigation_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB;

CREATE TABLE referral_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_visit_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    specialty VARCHAR(160) NOT NULL,
    provider_or_facility VARCHAR(200) NULL,
    clinical_reason TEXT NOT NULL,
    priority VARCHAR(16) NOT NULL,
    instructions TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_referral_visit_status (patient_visit_id, status),
    INDEX idx_referral_doctor_time (doctor_id, created_at),
    CONSTRAINT fk_referral_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id),
    CONSTRAINT fk_referral_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
) ENGINE=InnoDB;

CREATE TABLE clinical_order_audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    patient_visit_id BIGINT NOT NULL,
    resource_type VARCHAR(24) NOT NULL,
    resource_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_clinical_order_audit_visit_time (patient_visit_id, occurred_at),
    CONSTRAINT fk_clinical_order_audit_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_clinical_order_audit_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id)
) ENGINE=InnoDB;
