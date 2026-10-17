# GBK HealthCare Backend

Spring Boot REST API for the React patient-registration form and MySQL.

## Requirements
- JDK 21 or newer
- Maven 3.9+
- MySQL Server 8+

## 1. Create the database (optional)
The JDBC URL has `createDatabaseIfNotExist=true`, but you can create it yourself:
```sql
CREATE DATABASE gbk_healthcare;
```

## 2. Configure database credentials
Edit `src/main/resources/application.properties`, or set environment variables:
- Windows CMD: `set DB_USERNAME=root`
- Windows CMD: `set DB_PASSWORD=your_mysql_password`

Never commit real production credentials. The `change_me` password is a placeholder.

## 3. Run the backend
From this folder:
```bash
mvn spring-boot:run
```
API base URL: `http://localhost:8080/api/patients`

## 4. Test using Postman
POST `http://localhost:8080/api/patients`
Header: `Content-Type: application/json`
Body -> raw -> JSON:
```json
{
  "fullName": "Ravi Kumar",
  "age": 35,
  "gender": "Male",
  "visitDate": "2026-10-10",
  "phone": "9876543210",
  "email": "ravi@example.com",
  "temperature": 98.6,
  "pulseRate": 76,
  "spo2": 98,
  "bpSystolic": 120,
  "bpDiastolic": 80,
  "chiefComplaints": "Headache",
  "presentIllness": "Headache for two days",
  "generalExamination": "Alert",
  "heartExamination": "Normal findings",
  "lungsExamination": "Clear",
  "abdomenExamination": "Soft",
  "rectalExamination": "",
  "doctor": "Dr. Ramesh"
}
```
Expected: `201 Created` and JSON containing the generated `id`.
GET all: `GET http://localhost:8080/api/patients`
GET one: `GET http://localhost:8080/api/patients/1`

## Frontend
Copy the provided `frontend-integration/PatientRegistration.tsx` to your existing:
`frontend/src/components/PatientRegistration.tsx`
It posts the form to `http://localhost:8080/api/patients`. Keep your existing `PatientRegistration.css`.

## Important security note
This starter API has no authentication/authorization and is intended for local development only. Before deploying or storing real patient data, add authentication, role-based authorization, HTTPS, audit logging, privacy/security controls, and secret management. Do not expose this development API publicly.
