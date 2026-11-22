-- REVIEW ONLY. Do not execute as part of this code change.
-- Additive Phase 6 schema. Requires existing patient_visits, patients, staff_accounts,
-- admin_accounts tables and their BIGINT primary keys. Timestamps follow current OPD
-- LocalDateTime convention (DATETIME(6)); booleans use BIT(1).

CREATE TABLE billable_services (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(40) NOT NULL,
  name VARCHAR(160) NOT NULL,
  description TEXT NULL,
  category VARCHAR(24) NOT NULL,
  active BIT(1) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_billable_services_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE billable_service_prices (
  id BIGINT NOT NULL AUTO_INCREMENT,
  service_id BIGINT NOT NULL,
  version INT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  tax_rate_percent DECIMAL(5,2) NOT NULL,
  effective_at DATETIME(6) NOT NULL,
  created_by_admin_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_service_price_version UNIQUE (service_id, version),
  CONSTRAINT fk_service_price_service FOREIGN KEY (service_id) REFERENCES billable_services(id),
  CONSTRAINT fk_service_price_admin FOREIGN KEY (created_by_admin_id) REFERENCES admin_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE billing_invoices (
  id BIGINT NOT NULL AUTO_INCREMENT,
  patient_visit_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  invoice_number VARCHAR(48) NOT NULL,
  invoice_date DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  paid_amount DECIMAL(12,2) NOT NULL,
  outstanding_amount DECIMAL(12,2) NOT NULL,
  discount_status VARCHAR(20) NOT NULL,
  discount_requested_by_staff_id BIGINT NULL,
  discount_approved_by_admin_id BIGINT NULL,
  discount_approved_at DATETIME(6) NULL,
  created_by_staff_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_billing_invoice_number UNIQUE (invoice_number),
  INDEX idx_invoice_visit_status (patient_visit_id, status),
  INDEX idx_invoice_patient_date (patient_id, invoice_date),
  CONSTRAINT fk_invoice_visit FOREIGN KEY (patient_visit_id) REFERENCES patient_visits(id),
  CONSTRAINT fk_invoice_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_invoice_discount_request_staff FOREIGN KEY (discount_requested_by_staff_id) REFERENCES staff_accounts(id),
  CONSTRAINT fk_invoice_discount_approver FOREIGN KEY (discount_approved_by_admin_id) REFERENCES admin_accounts(id),
  CONSTRAINT fk_invoice_creator FOREIGN KEY (created_by_staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE billing_invoice_lines (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  service_id BIGINT NULL,
  price_version_id BIGINT NULL,
  service_code_snapshot VARCHAR(40) NOT NULL,
  service_name_snapshot VARCHAR(160) NOT NULL,
  quantity DECIMAL(8,2) NOT NULL,
  unit_price_snapshot DECIMAL(10,2) NOT NULL,
  tax_rate_percent_snapshot DECIMAL(5,2) NOT NULL,
  tax_amount_snapshot DECIMAL(10,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_invoice_line_invoice (invoice_id),
  INDEX idx_invoice_line_service (service_id),
  INDEX idx_invoice_line_price_version (price_version_id),
  CONSTRAINT fk_invoice_line_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoices(id),
  CONSTRAINT fk_invoice_line_service FOREIGN KEY (service_id) REFERENCES billable_services(id),
  CONSTRAINT fk_invoice_line_price FOREIGN KEY (price_version_id) REFERENCES billable_service_prices(id)
) ENGINE=InnoDB;

CREATE TABLE billing_payments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  receipt_number VARCHAR(48) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  method VARCHAR(16) NOT NULL,
  method_label VARCHAR(80) NULL,
  payment_reference VARCHAR(180) NULL,
  paid_at DATETIME(6) NOT NULL,
  recorded_by_staff_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_payment_receipt_number UNIQUE (receipt_number),
  INDEX idx_payment_invoice_time (invoice_id, paid_at),
  CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoices(id),
  CONSTRAINT fk_payment_staff FOREIGN KEY (recorded_by_staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE billing_refunds (
  id BIGINT NOT NULL AUTO_INCREMENT,
  payment_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  original_payment_reference VARCHAR(180) NULL,
  refund_reference VARCHAR(180) NULL,
  refunded_by_staff_id BIGINT NOT NULL,
  refunded_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_refund_payment_time (payment_id, refunded_at),
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES billing_payments(id),
  CONSTRAINT fk_refund_staff FOREIGN KEY (refunded_by_staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE billing_adjustments (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  created_by_staff_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_adjustment_invoice_time (invoice_id, created_at),
  CONSTRAINT fk_adjustment_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoices(id),
  CONSTRAINT fk_adjustment_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff_accounts(id)
) ENGINE=InnoDB;

CREATE TABLE billing_audit_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT NULL,
  actor_role VARCHAR(16) NOT NULL,
  actor_id BIGINT NOT NULL,
  actor_email VARCHAR(160) NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  resource_type VARCHAR(24) NOT NULL,
  resource_id BIGINT NOT NULL,
  occurred_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_billing_audit_invoice_time (invoice_id, occurred_at),
  CONSTRAINT fk_billing_audit_invoice FOREIGN KEY (invoice_id) REFERENCES billing_invoices(id)
) ENGINE=InnoDB;
