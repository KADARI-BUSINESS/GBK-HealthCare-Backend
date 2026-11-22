# Doctor activity table provisioning

The backend validates the production schema and will not create this table.
Production execution requires separate, explicit change approval, a verified
backup, and a reviewed target schema. This document and a production readiness
review do not themselves authorize execution. Apply it before deploying a
backend build that contains the activity feature. The script only creates a
table and does not change existing rows.

From the backend project root, use the MySQL-compatible TiDB client:

```powershell
mysql --host=<TIDB_HOST> --port=4000 --user=<DB_USER> --password --database=<DB_NAME> < doctor_activity_migration.sql
```

The client prompts for the database password. Replace the three placeholders;
do not put credentials in source control or in the command history. The
migration account needs `CREATE` on the target database (or narrower approved
scope) for `doctor_activity`.

The SQL uses `CREATE TABLE IF NOT EXISTS`. It can be invoked again without
failing solely because the table exists, but it does not validate or repair an
existing table's columns or index. After an interrupted or uncertain execution,
inspect `SHOW CREATE TABLE` and `SHOW INDEX` first. If the definition is partial
or incorrect, stop and obtain a separately reviewed forward repair; do not
assume rerunning the file repairs it.

Verify the table and index after execution:

```sql
SHOW CREATE TABLE doctor_activity;
SHOW INDEX FROM doctor_activity;
```

The index is `(doctor_id, occurred_at, id)`. TiDB does not support descending
index keys; the bounded activity query sorts by `occurred_at DESC, id DESC` and
can scan the composite index in reverse. Confirm the plan with `EXPLAIN` in the
target TiDB version before production rollout.
