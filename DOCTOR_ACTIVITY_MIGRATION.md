# Doctor activity table provisioning

The backend validates the production schema and will not create this table. Run
the SQL once against the target TiDB database before deploying a backend build
that contains the activity feature. No existing table or row is changed.

From the backend project root, use the MySQL-compatible TiDB client:

```powershell
mysql --host=<TIDB_HOST> --port=4000 --user=<DB_USER> --password --database=<DB_NAME> < doctor_activity_migration.sql
```

The client prompts for the database password. Replace the three placeholders;
do not put credentials in source control or in the command history.

Verify the table and index after execution:

```sql
SHOW CREATE TABLE doctor_activity;
SHOW INDEX FROM doctor_activity;
```

The index is `(doctor_id, occurred_at, id)`. TiDB does not support descending
index keys; the bounded activity query sorts by `occurred_at DESC, id DESC` and
can scan the composite index in reverse. Confirm the plan with `EXPLAIN` in the
target TiDB version before production rollout.
