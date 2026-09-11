# Star International School: 18-module audit

Baseline recorded 2026-09-05 **before major implementation changes**. Primary requirements are the supplied seven-page Super Admin Blueprint; the user's pasted request supplies additional workflows and execution requirements. Architecture remains one Spring Boot modular monolith, Flutter client, PostgreSQL database and Flyway history. Existing user edits and supplied originals are preserved.

The detailed tables linked below contain backend, frontend, database, CRUD, API, RBAC, integration, known issues, missing requirements and recommended action for **every module**, with screen → datasource → controller → service → repository → table traces. COMPLETE requires demonstrated requested workflows; compilation, menus and generic record CRUD do not qualify. Status categories are mutually exclusive; a partial module can still have individual broken workflows.

| # | Module | Baseline status | Detailed audit |
| --- | --- | --- | --- |
| 1 | Dashboard | NEEDS REFACTORING | [Dashboard/report/site trace](dashboard-library-reports-website.md) |
| 2 | User & Role Management | NEEDS REFACTORING | [Security/settings trace](security-settings-backup.md) |
| 3 | Academic Management | NEEDS REFACTORING | [Academic/student trace](academic-student-staff.md) |
| 4 | Student Management | PARTIALLY COMPLETE | [Academic/student trace](academic-student-staff.md) |
| 5 | Staff Management | PARTIALLY COMPLETE | [Academic/student trace](academic-student-staff.md) |
| 6 | Parent Management | PARTIALLY COMPLETE | [Academic/student trace](academic-student-staff.md) |
| 7 | Examination | PARTIALLY COMPLETE | [Academic/student trace](academic-student-staff.md) |
| 8 | Attendance | PARTIALLY COMPLETE | [Academic/student trace](academic-student-staff.md) |
| 9 | Transport | EXISTS BUT BROKEN | [Fees/allocation trace](fees-hostel-transport.md) |
| 10 | Hostel | EXISTS BUT BROKEN | [Fees/allocation trace](fees-hostel-transport.md) |
| 11 | Fees & Accounts | EXISTS BUT BROKEN | [Fees/allocation trace](fees-hostel-transport.md) |
| 12 | Library | NOT IMPLEMENTED | [Dashboard/report/site trace](dashboard-library-reports-website.md) |
| 13 | Communication | PARTIALLY COMPLETE | [Dashboard/report/site trace](dashboard-library-reports-website.md) |
| 14 | Reports | EXISTS BUT BROKEN | [Dashboard/report/site trace](dashboard-library-reports-website.md) |
| 15 | Apps & Portals | PARTIALLY COMPLETE | [Dashboard/report/site trace](dashboard-library-reports-website.md) |
| 16 | System Settings | PARTIALLY COMPLETE | [Security/settings trace](security-settings-backup.md) |
| 17 | Backup & Restore | NOT IMPLEMENTED | [Security/settings trace](security-settings-backup.md) |
| 18 | Audit & Security | NEEDS REFACTORING | [Security/settings trace](security-settings-backup.md) |

**Baseline totals:** total 18; COMPLETE **0**; PARTIALLY COMPLETE **8**; NOT IMPLEMENTED **2**; EXISTS BUT BROKEN **4**; NEEDS REFACTORING **4**. Total still requiring work: **18**. Certificates belong to Student Management; the public website is an additional deliverable, neither adds a nineteenth module.

## Baseline validation

- Java 21 `mvn -q test`: **95 tests, 23 suites, 0 failures/errors/skips** on 2026-09-05. Existing tests predominantly use mocked services; test profile disables Flyway and uses H2 without schema creation. This result does not validate PostgreSQL migrations or full workflows.
- Flutter analysis started; result pending at baseline.
- No production database mutation, deployment or restore performed. PostgreSQL 17 tools are available for an isolated migration/transaction test environment.

## Implementation sequence from evidence

1. Authentication and effective permission/status enforcement; persist failed-login counters and audit; remove settings authority from role management; make custom role assignment usable.
2. Student class-history and parent identity integrity; exam schedule time/room fields; consistent academic teacher mapping and historical certificate generation.
3. Financial discount/payment/cancellation invariants, complete lists/exports, capacity serialization and transfer ordering; preserve paid financial history during allocation reconciliation.
4. Report filtering/canonical data sources and dashboard access/data integrity.
5. Public website local optimized supplied photos, semantic descriptions and honest labeled facility placeholders.
6. Run expanded backend/frontend and disposable PostgreSQL regression, then publish final changed files/API/migration/workflow evidence and prioritized remaining requirements. Work items remain pending until tested; this baseline is retained unchanged as the before state.
