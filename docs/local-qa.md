# Local QA

Use the `localqa` profile when you want a repeatable demo and smoke-test environment without PostgreSQL or Docker.

It starts the API with in-memory H2 and seeds:

- `SUPER_ADMIN` user: `admin@school.test` / `Admin@12345678`
- demo users for principal, teacher, accountant, receptionist, and warden using `Demo@12345678`
- roles and permissions
- five active students: `ADM-2026-0001` through `ADM-2026-0005`
- fee categories
- one active Class 6 A fee structure
- five student fee assignments
- demo fee collections and audit log events

Start the backend:

```powershell
.\scripts\start-local-qa-backend.ps1
```

Run smoke tests:

```powershell
.\scripts\local-qa-smoke.ps1
```

Run the backend, smoke test it, and stop it in one command:

```powershell
.\scripts\run-local-qa-e2e.ps1
```

Useful URLs:

```text
http://localhost:8080/api/swagger-ui.html
http://localhost:8080/api/v1/system/status
http://localhost:8080/api/actuator/health
```

The local QA database is in memory, so data resets when the backend stops.
