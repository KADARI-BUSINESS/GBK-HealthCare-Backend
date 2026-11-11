# GBK HealthCare Backend

Spring Boot REST API for GBK HealthCare.

## Requirements

- JDK 17
- Maven 3.9+
- TiDB Cloud cluster (MySQL compatible)

## Database configuration

The application uses the TiDB Cloud JDBC endpoint configured in
`src/main/resources/application.properties`. The connection URL and database
name are already configured there. Running the backend with these settings
connects to that TiDB cluster; use it only when that is intended. Do not replace
the configured endpoint with local MySQL values when running against the
production cluster.

Set these environment variables before starting the backend:

- `DB_USERNAME`: the exact SQL username shown in the TiDB Cloud cluster's
  Connect dialog. TiDB Cloud Starter/Essential shared endpoints require the
  generated cluster username prefix (for example, `<cluster-prefix>.root` for
  the root SQL account); `root` alone will fail for those endpoints.
- `DB_PASSWORD`: the SQL password for that username.
- `JWT_SECRET_BASE64`: a Base64-encoded random signing key containing at least
  32 bytes. Keep it private and consistent across backend instances so issued
  JWTs remain verifiable.

Use the credentials and TLS connection details supplied by TiDB Cloud. Never
put real database or JWT secrets in source control. In PowerShell, assign the
three environment variables for the current terminal session before running
Maven (for example, `$env:DB_USERNAME = '<username from TiDB Connect>'`).

## Run locally

From this directory, after setting the required environment variables:

```powershell
mvn spring-boot:run
```

The default backend port is `8080` (`PORT` can override it). The frontend's
local API default is `http://localhost:8080`; set `VITE_API_BASE_URL` to
override it.

## Authentication and authorization

Doctor login is available at `POST /api/auth/doctor/login` and accepts the
doctor's email and password. On success, the response contains a signed JWT.
All other `/api/**` routes require that token in the request header:

```http
Authorization: Bearer <doctor-jwt>
```

The backend is stateless and authorizes API requests with the `SCOPE_DOCTOR`
authority. Patient, visit, follow-up, report, and dashboard data access is
scoped to the authenticated doctor. The login endpoint is public; CORS preflight
`OPTIONS` requests are also permitted.

The optional doctor bootstrap runner is disabled by default. Configure its
environment variables only when intentionally provisioning an initial doctor;
never commit bootstrap credentials.

## Dashboard statistics

`GET /api/dashboard/stats` requires an authenticated doctor JWT and returns
database aggregates. Patient and visit counts are scoped to the authenticated
doctor. Pending follow-ups include only `SCHEDULED` records dated today or
later and linked to that doctor's patient. Completed and cancelled follow-ups
are excluded. Today's visits use the application timezone configured by
`app.timezone` (default `Asia/Kolkata`). Clinical notes count visits with at
least one nonblank clinical text field or a recorded vital sign.

## Schema management

Hibernate is currently configured with `spring.jpa.hibernate.ddl-auto=update`.
At startup, Hibernate may alter tables to match entity mappings. This can make
schema changes without a reviewed migration and should be treated carefully for
production data. Do not change this setting without first agreeing on a schema
migration strategy and reviewing the consequences; this project currently has
no migration-based schema tool configured.
