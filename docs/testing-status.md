# Testing Status

## Verified Now

- Backend compiles with Java 21.
- Full backend unit test suite passes.
- Backend MockMvc security regression tests cover protected auth, student, and fee routes.
- Controller authorization guard test verifies protected controller handlers declare `@PreAuthorize` unless explicitly public.
- Fee negative-path unit coverage includes duplicate fee structures, overpayment, and non-cash payment reference validation.
- Flutter `pub get`, `analyze`, and widget/unit tests pass with Flutter 3.44.0 and Dart 3.12.0.
- CI workflow exists for Java tests, Flutter analysis/tests, and local QA API smoke tests.
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

## Executed On 2026-05-27

- Restored Maven Spring Boot parent version so backend tests can run.
- Ran backend tests with JDK 21: `./mvnw -q test`.
- Ran Flutter checks: `flutter --no-version-check pub get`, `flutter --no-version-check analyze`, and `flutter --no-version-check test`.
- Ran local QA API smoke on port 18080: login, protected reads, payment collection, receipt lookup, and defaulter report passed.
- Updated stale Flutter login widget assertion to match the current UI text.
- Added `.github/workflows/ci.yml` with backend tests, frontend checks, and API smoke coverage.

## Pending Before Production Sign-Off

- Run backend against real PostgreSQL with Flyway migrations, not only the in-memory local QA profile.
- Browser-test the admin panel on desktop and mobile breakpoints.
- Expand controller/integration tests with PostgreSQL-backed Testcontainers flows for auth, students, and fees.
- Add full role-permission matrix regression tests for every protected API, beyond the authorization annotation guard.
- Add browser-level negative-path E2E tests for validation, duplicate structures, overpayment, and non-cash payment references.
- Add observability hardening: structured logs, metrics dashboards, request tracing, and audit export.
- Add backup/restore and seed-data reset strategy for staging.
