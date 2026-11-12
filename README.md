# GBK HealthCare Backend

Spring Boot REST API for GBK HealthCare.

## Requirements

- JDK 17
- Maven 3.9+
- TiDB Cloud cluster (MySQL compatible)

## Database configuration

The application reads its JDBC connection URL and credentials from environment
variables. Set the TiDB Cloud connection URL in `DB_URL`; do not put the URL or
database credentials in source-controlled properties files.

Set these environment variables before starting the backend:

- `DB_USERNAME`: the exact SQL username shown in the TiDB Cloud cluster's
  Connect dialog. TiDB Cloud Starter/Essential shared endpoints require the
  generated cluster username prefix (for example, `<cluster-prefix>.root` for
  the root SQL account); `root` alone will fail for those endpoints.
- `DB_PASSWORD`: the SQL password for that username.
- `DB_URL`: the TiDB Cloud JDBC URL, including the database name and TLS options
  supplied for the cluster.
- `JWT_SECRET_BASE64`: a Base64-encoded random signing key containing at least
  32 bytes. Keep it private and consistent across backend instances so issued
  JWTs remain verifiable.

Use the TLS connection details supplied by TiDB Cloud. Never put real database,
JWT, or SMTP secrets in source control. Report email delivery also uses
`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, and `MAIL_APP_PASSWORD`; the username
and app password must be set in the runtime environment when email delivery is
enabled.

## Run locally

From this directory, after setting the required environment variables:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'local'
mvn spring-boot:run
```

The local profile keeps schema updates enabled for development and uses local
cookie/CORS settings. The backend listens on port `5000` locally; the frontend
defaults to `http://localhost:5000`. Set `PORT` to override the backend port or
`VITE_API_BASE_URL` to override the frontend API URL.

For production, activate the `prod` profile with
`SPRING_PROFILES_ACTIVE=prod`. It validates the pre-provisioned schema and does
not return internal exception messages. Provision required schema changes
through the approved database process before starting the application; the
production profile does not create or update tables. The server honors the
platform-provided `PORT` value and falls back to `5000`.

## Authentication and authorization

Doctor login is available at `POST /api/auth/doctor/login` and accepts the
doctor's email and password. On success, the response contains a short-lived
signed JWT and sets a persistent, HttpOnly refresh cookie. Refresh secrets are
stored only as hashes in the `doctor_refresh_sessions` table. All data API calls
continue to require the access token in the request header:

```http
Authorization: Bearer <doctor-jwt>
```

`POST /api/auth/doctor/refresh` rotates the refresh cookie and returns a new
access token plus a CSRF value. `POST /api/auth/doctor/logout` revokes the
refresh-token family and clears both auth cookies. Both endpoints require the
CSRF value in the `X-XSRF-TOKEN` header and use credentialed requests. On a
fresh app load, `GET /api/auth/doctor/csrf` bootstraps a CSRF value so the app
can call refresh. The CSRF value is safe for JavaScript; the refresh secret
never appears in a response body. The refresh cookie is HttpOnly, Secure,
SameSite=None, and scoped to the doctor auth path. Browser clients must include
credentials on login, CSRF bootstrap, refresh, and logout.
Access tokens expire after 15 minutes by default; refresh sessions expire after
30 days by default. Set `JWT_EXPIRATION_SECONDS` or
`REFRESH_EXPIRATION_SECONDS` to override those durations.
`AUTH_ALLOWED_ORIGINS` controls browser origins; set it to the exact production
frontend origin(s) in production. Its default includes the local Vite app and
the configured Amplify frontend.

The backend is stateless for data API authorization and uses the `SCOPE_DOCTOR`
authority. Patient, visit, follow-up, report, and dashboard data access remains
scoped to the authenticated doctor. Login, refresh, and logout are public to the
security filter chain; cookie endpoints enforce CSRF with the double-submit
token. CORS preflight `OPTIONS` requests are also permitted.

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

The local profile uses `spring.jpa.hibernate.ddl-auto=update` for development.
The production profile uses `validate`, so startup fails if the pre-provisioned
database schema does not match the application entities. This project currently
has no migration-based schema tool configured; schema changes must be reviewed
and provisioned separately before production deployment.
