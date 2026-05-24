# Testing Status

## Verified Now

- Backend compiles with Java 21.
- Full backend unit test suite passes.
- Local QA backend boots with seeded master data.
- Smoke test passes for:
  - login
  - current user context
  - student listing
  - fee category listing
  - fee structure listing
  - student fee assignment listing
  - payment collection
  - receipt lookup
  - defaulter report listing

## Pending Before Production Sign-Off

- Run backend against real PostgreSQL with Flyway migrations, not only the in-memory local QA profile.
- Run Flutter `pub get`, `analyze`, and widget tests once Flutter is installed.
- Browser-test the admin panel on desktop and mobile breakpoints.
- Add controller/integration tests for auth, students, and fees using MockMvc/Testcontainers.
- Add role-permission regression tests for every protected API.
- Add negative-path E2E tests for validation, duplicate structures, overpayment, and non-cash payment references.
- Add CI pipeline steps for Java tests, Flutter analysis/tests, and API smoke tests.
- Add observability hardening: structured logs, metrics dashboards, request tracing, and audit export.
- Add backup/restore and seed-data reset strategy for staging.
