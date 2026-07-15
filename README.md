# School ERP

Production-ready foundation for a modular monolith School ERP.

## Stack

- Backend: Java 21, Spring Boot 3.5.12, PostgreSQL, Spring Security JWT, JPA/Hibernate, Flyway, OpenAPI
- Frontend: Flutter 3.44 stable, Riverpod, Dio, GoRouter, clean architecture

## Backend Layout

- `com.school.erp.common`: shared API envelopes, pagination, domain base classes, mapper contract, security, web filters, exceptions
- `com.school.erp.config`: infrastructure configuration
- `com.school.erp.modules`: bounded ERP module packages: `auth`, `users`, `students`, `academic`, `fees`, `hostel`, `attendance`, `reports`, `notifications`, `settings`
- `src/main/resources/db/migration`: Flyway migrations
- `docs/backend-architecture.md`: modular monolith rules and future extraction guidance

## Frontend Layout

- `frontend/lib/src/app`: app shell and routing
- `frontend/lib/src/core`: config, networking, storage, result/failure primitives
- `frontend/lib/src/features`: feature modules and dashboard shell

## Local Backend

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Swagger UI is available at:

```text
http://localhost:8080/api/swagger-ui.html
```

Health/status endpoints:

```text
http://localhost:8080/api/actuator/health
http://localhost:8080/api/v1/system/status
```

Auth endpoints:

```text
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
POST /api/v1/auth/change-password
GET  /api/v1/auth/me
```

Initial `SUPER_ADMIN` creation is opt-in. Set `BOOTSTRAP_SUPER_ADMIN_ENABLED=true`
with `BOOTSTRAP_SUPER_ADMIN_EMAIL` and a strong `BOOTSTRAP_SUPER_ADMIN_PASSWORD`.

## Local Frontend

After installing Flutter stable:

```bash
cd frontend
flutter pub get
flutter run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

### Public Admission Enquiry Email

The Flutter Web public admission enquiry form posts to the Spring Boot backend:

```text
POST /api/v1/public/enquiries
```

The backend reuses the existing notification email provider. Configure the
school copy recipient with:

```yaml
school:
  public-site:
    enquiry-email: info@starinternationalschool.com
```

Run the frontend against the local backend:

```bash
flutter run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

## Local QA

For a disposable demo backend with seeded auth, student, and fee data:

```powershell
.\scripts\start-local-qa-backend.ps1
.\scripts\local-qa-smoke.ps1
```

Or run the backend, smoke test it, and stop it in one command:

```powershell
.\scripts\run-local-qa-e2e.ps1
```

See `docs/local-qa.md` for seeded credentials and test data.

Full local run instructions are in `docs/run-application.md`.
Audit log API examples are in `docs/audit-log-api.md`.
