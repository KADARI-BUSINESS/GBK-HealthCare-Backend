-- Provision the append-only doctor activity log before deploying the backend.
-- TiDB does not support descending index keys, so this composite index is
-- created in ascending order; the activity query requests a reverse sort.
CREATE TABLE IF NOT EXISTS doctor_activity (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_doctor_activity_doctor_time_id (doctor_id, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
