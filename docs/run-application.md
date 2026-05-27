# Run The Application

This guide covers the fastest local demo path and the PostgreSQL path for the School ERP modular monolith.

## Prerequisites

- Java 21. This machine has JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- Flutter stable. This machine has Flutter at `C:\Program Files\flutter`.
- Chrome for the Flutter web admin panel.
- Docker Desktop only if you want to run PostgreSQL locally.

If `flutter` is not recognized in a new terminal, refresh the terminal or add Flutter to the current PowerShell session:

```powershell
$env:Path = "C:\Program Files\flutter\bin;$env:Path"
flutter --version
```

You can always use the full path:

```powershell
& "C:\Program Files\flutter\bin\flutter.bat" --version
```

## Quick Demo With Seeded Master Data

Use this path for product testing. It runs the backend with the `localqa` profile, H2 in-memory database, and seeded master data.

Seeded login:

```text
Email: admin@school.com
Password: Admin@12345678
```

The app also keeps `admin@school.test` with the same password for local QA compatibility.

Seeded data:

- Roles and permissions
- Database-backed demo users:
  `admin@school.com`, `admin@school.test`, `principal@school.com`,
  `teacher1@school.com`, `accountant@school.com`, `reception@school.com`,
  `warden@school.com`
- Passwords:
  admin users use `Admin@12345678`; other demo users use `Demo@12345678`
- Five active demo students
- Fee categories
- One Class 6 A fee structure
- Five student fee assignments
- Seeded fee collections and audit log events

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-local-qa-backend.ps1
```

Backend URLs:

```text
API base:   http://localhost:8080/api
Swagger:    http://localhost:8080/api/swagger-ui.html
Health:     http://localhost:8080/api/actuator/health
Status:     http://localhost:8080/api/v1/system/status
```

The default access-token session timeout is 60 minutes. Override it per environment with `JWT_ACCESS_TOKEN_TTL`, for example `$env:JWT_ACCESS_TOKEN_TTL = "2h"`.

In a second terminal, start the Flutter admin panel:

```powershell
cd frontend
flutter pub get
flutter run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

If Flutter is still not on PATH:

```powershell
cd frontend
& "C:\Program Files\flutter\bin\flutter.bat" pub get
& "C:\Program Files\flutter\bin\flutter.bat" run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

The fixed `8081` web port matters because it is already allowed by backend CORS.

## One Command Smoke Test

To start the seeded backend, exercise login, users, students, fees, audit logs, receipt creation, and then stop the backend:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-qa-e2e.ps1
```

Use this before UI testing to confirm the API is healthy.

## PostgreSQL Backend

Use this path when you want Flyway migrations against PostgreSQL.

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Start the backend:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.4"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/school_erp"
$env:SPRING_DATASOURCE_USERNAME = "school_erp"
$env:SPRING_DATASOURCE_PASSWORD = "school_erp"
$env:JWT_SECRET = "replace-this-with-a-strong-32-character-secret"
$env:JWT_ACCESS_TOKEN_TTL = "60m"
$env:BOOTSTRAP_SUPER_ADMIN_ENABLED = "true"
$env:BOOTSTRAP_SUPER_ADMIN_EMAIL = "admin@school.test"
$env:BOOTSTRAP_SUPER_ADMIN_PASSWORD = "Admin@12345678"
.\mvnw spring-boot:run
```

Then run the Flutter admin panel from `frontend`:

```powershell
flutter run -d chrome --web-port=8081 --dart-define=API_BASE_URL=http://localhost:8080/api
```

## Validation Commands

Backend:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.4"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw test
```

Frontend:

```powershell
cd frontend
flutter pub get
flutter analyze
flutter test
```

Flutter doctor:

```powershell
flutter doctor -v
```

For web admin testing, Chrome must be available. Android SDK and Visual Studio desktop workloads are optional unless you plan to build Android or Windows desktop apps.

## Stop Local Processes

If the backend was started by `start-local-qa-backend.ps1`, stop the printed process id:

```powershell
Stop-Process -Id <PID> -Force
```

Or stop anything listening on backend port `8080`:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force
}
```

Stop the Flutter dev server with `Ctrl+C` in its terminal.
