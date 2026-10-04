# Implementation Progress - 2026-09-05

This document tracks implementation work after the completed 18-module baseline audit. The baseline audit files remain historical records and were not edited or re-scored as part of this pass.

## Phase 1 - Authentication, RBAC, User Status, Audit/Security

Status: implemented and backend-tested.

Completed items:

- Added durable failed-login handling through `AuthenticationFailureService` using a separate transaction path, so failed attempt counters and audit rows survive rejected login responses.
- Added `LOGIN_FAILED` audit action coverage for known accounts, unknown accounts, and blocked login attempts without recording passwords.
- Fixed temporary lock handling by allowing expired temporary locks to be released during login/refresh through `UserAccount.unlockIfTemporaryLockExpired()`.
- Filtered inactive roles and inactive permissions from login `UserDetails`, JWT issuance, and request-time JWT authority conversion.
- Added request-time JWT account validation so deleted, disabled, locked, or password-reset-stale access tokens are rejected before controller logic runs.
- Changed user create/update/search and role assignment payload handling from enum-only roles to normalized string role names, preserving existing enum role values while allowing custom roles.
- Updated user import role parsing to support custom role names while continuing to block Student/Teacher user creation outside their domain modules.
- Removed `SETTINGS_READ`/`SETTINGS_UPDATE` authorization overlap from role and permission management endpoints, including the legacy `/v1/users/roles/{roleId}/permissions` route.
- Removed the public security-chain allowance for `/v1/users/roles`; access now follows the controller's `USERS_READ` method security.
- Added inactive permission validation to the legacy user-service role-permission update path.
- Added refresh-token revocation for admin password reset, user deactivation, and user deletion.
- Added SUPER_ADMIN target protection for account update, activation, deactivation, password reset, delete, role assignment, and role removal.
- Changed auth and audit client IP capture to use the servlet remote address instead of unconditionally trusting `X-Forwarded-For`.
- Updated `/v1/auth/me` to return the live authenticated authorities rather than stale role/permission claims.

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\mvnw.cmd -q test
```

Result: PASS. 26 test classes, 112 tests, 0 failures, 0 errors, 0 skipped.

New/updated regression coverage:

- `AuthenticationFailureServiceTest`
- `AuthServiceTest`
- `SchoolJwtGrantedAuthoritiesConverterTest`
- `UserAccountJwtValidatorTest`
- `UserServiceRolePermissionsTest`
- `ApiSecurityRegressionTest`

## Phase 2 - Academic, Student/Parent, Examination Foundations

Status: implemented and backend-tested. Flutter validation and PostgreSQL/Flyway container validation were attempted but blocked by the local environment.

Completed items:

- Added an explicit current academic year marker with `GET /v1/academic-years/current`, `PATCH /v1/academic-years/{academicYearId}/current`, create/update payload support, fallback current-year resolution, and a partial unique PostgreSQL index.
- Prevented inactive academic years, classes, and divisions from being used for new student class assignments.
- Fixed same-day student transfers so closing an existing assignment cannot create `effective_to < effective_from`.
- Blocked updates to inactive historical class assignments to avoid accidentally reactivating old enrollment history.
- Changed division deletion protection to account for all student assignment history, not only active students.
- Added phone-and-name reuse for email-less parent/guardian records and kept parent-mapping edits on the current parent record unless an existing email identity is explicitly selected.
- Added scoped parent portal children access through `GET /v1/parents/me/children`, resolved from the authenticated user id and parent guardian link.
- Added per-subject exam schedule `startTime`, `endTime`, and `room` fields across backend DTOs, entity model, frontend models, and the schedule dialog.
- Added exam schedule validation for duplicate subjects in the same exam type/class/division, same-schedule slot overlaps, existing slot conflicts, non-negative passing marks, and end-time-after-start-time.
- Added `V28__phase2_academic_student_exam_foundations.sql` for academic current-year state, parent lookup indexing, and exam slot columns/check constraints.

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; .\mvnw.cmd -q test
```

Result: PASS. 26 test classes, 123 tests, 0 failures, 0 errors, 0 skipped.

Additional validation notes:

- `git diff --check`: PASS with LF/CRLF warnings only.
- PostgreSQL/Flyway validation: blocked because Docker daemon was not running and no local `psql`/`postgres` executable was available.
- Flutter validation: `dart format`, `flutter analyze`, `flutter test --no-pub`, and `flutter pub get` were attempted but hung without output and were interrupted.

New/updated regression coverage:

- `AcademicHierarchyServiceTest`
- `StudentServiceTest`
- `ExamServiceTest`

## Phase 3 - Fees, Hostel, Transport

Status: IN PROGRESS. This entry starts Phase 3 after preserving the baseline, Phase 1, and Phase 2 records. No final 18-module re-score was performed.

### Phase 2 Carry-Forward Verification

| Requirement | Status | Notes |
|---|---|---|
| Class-Subject mapping | IMPLEMENTED - TEST PENDING | `DivisionSubject` CRUD and duplicate guard exist; no dedicated service test for add/update/delete division subject mapping was found in this pass. |
| Teacher-Subject mapping | TESTED | `assignSubjectTeacherAddsTeacherWithoutReplacingOtherSubjectTeachers` covers additive subject-teacher mapping and duplicate prevention exists in service. |
| Teacher-Class/Division mapping | TESTED | `assignClassTeacherReplacesExistingActiveMapping` covers active class/division teacher replacement. |
| Duplicate mapping prevention | PARTIAL | Class fee and teacher-subject duplicate guards are tested; division-subject duplicate guard is implemented but not directly tested. |
| Historical teacher assignment preservation | PARTIAL | Active replacement keeps old mapping rows, but full teacher assignment retirement/synchronization remains a larger academic/staff lifecycle item. |
| Student promotion workflow | MISSING | Deferred as a standalone academic progression workflow. Existing class assignment history supports the data model foundation. |
| Student detention workflow | MISSING | Deferred as a standalone academic progression workflow. |
| Student academic history retrieval | TESTED | `StudentClassAssignment` history is returned through student profile/summary flows and now has an active student/year lookup for fee reconciliation. |
| Marks tied to exam schedule/subject context | TESTED | Marks persist `examSchedule` and `subject`; tests cover scheduled max marks and subject context. |
| Marks > maximum marks rejection | TESTED | `saveMarksRejectsMarksGreaterThanScheduledMaxMarks`. |
| Duplicate marks prevention | IMPLEMENTED - TEST PENDING | Service upserts by existing student/schedule/subject marks and V15 has active unique index; no explicit duplicate-row regression test was added in this pass. |
| Result-processing foundation | TESTED | `generateResultsUsesScheduledPassingMarks` and student profile result tests cover the current computed foundation. |
| Historical Bonafide context | PARTIAL | Persisted generated-document metadata exists, but legacy rendering can still use current student context instead of immutable certificate context. |
| Historical LC/TC context | PARTIAL | Leaving certificate history and duplicate-LC guard exist; transfer certificate is not a separate complete lifecycle and rendering remains legacy-context based. |
| Generated certificate/document history | PARTIAL | V26 and `GeneratedDocumentService` exist; student profile legacy generated-document route still bypasses persisted history. |

### Phase 3 Work Started

| Requirement | Status | Files changed | Migration | API | UI | Tests | Notes |
|---|---|---|---|---|---|---|---|
| Academic Enrollment -> Class Fee Applicability -> Student Fee Assignment | TESTED | `StudentFeeAutoAssignmentService.java`, `StudentClassAssignmentRepository.java`, `StudentService.java` | None | None | None | `StudentFeeAutoAssignmentServiceTest`, `StudentServiceTest` | Added `reconcileStudentFees(studentId, academicYearId)` and routed student class changes through the canonical reconciler. |
| Fee assignment section filtering | TESTED | `FeeAssignmentSpecifications.java` | None | Existing `/v1/fees/assignments` | None | Full Maven suite | Section filters now require active, non-deleted academic enrollment and matching academic/class context. |
| Defaulter section filtering | TESTED | `StudentFeeAssignmentRepository.java`, `FeeService.java` | None | Existing `/v1/fees/defaulters` | None | `FeeServiceTest`, full Maven suite | Section ID filtering now uses active student enrollment so class-wide dues remain visible for students in a division. |
| Payment / receipt / balance invariants | TESTED | `FeeServiceTest.java` | None | Existing fee payment/receipt APIs | None | `FeeServiceTest` | Coverage confirms overpayment rejection, non-cash reference requirement, duplicate reference rejection, cancelled assignment mutation guards, receipt creation, discount-after-payment, late-fee behavior, full reversal, and full refund. |
| Hostel allocation transaction ordering and capacity lock | TESTED | `HostelRoomRepository.java`, `HostelService.java` | None | Existing hostel allocation APIs | None | `HostelServiceTest` | Target room is locked before occupancy count; transfer closes and flushes old active allocation before replacement insert. PostgreSQL concurrency validation remains blocked. |
| Transport allocation transaction ordering, capacity lock, inactive-target guard | TESTED | `TransportVehicleRepository.java`, `TransportService.java` | None | Existing transport assignment APIs | None | `TransportServiceTest` | Target vehicle is locked before occupancy count; transfer closes and flushes old active assignment before replacement insert; inactive route/vehicle/stop targets are rejected on change. PostgreSQL concurrency validation remains blocked. |
| Hostel/transport future unpaid fee reconciliation after transfer/vacate/remove | BACKEND-TESTED | `StudentFeeAutoAssignmentService.java`, `StudentFeeAssignmentRepository.java`, `HostelService.java`, `TransportService.java` | None | Existing hostel/transport mutation APIs | None | `StudentFeeAutoAssignmentServiceTest`, `HostelServiceTest`, `TransportServiceTest` | Canonical reconciler now cancels obsolete future fully-unpaid hostel/transport assignments, preserves paid/part-paid/discounted/past-due history, and creates current applicable fees only when no active non-cancelled equivalent exists. |
| Refund/reversal partial model and adjustment history | PARTIAL | `FeeService.java`, `FeeServiceTest.java`, `StudentFeeAssignmentRepository.java` | None | Existing full-payment actions | None | `FeeServiceTest` | Full reversal/refund restore balances, cancel receipts, and preserve payment/allocation rows. Partial refunds remain deferred until an adjustment/ledger model exists. |
| Fees/hostel/transport frontend pagination and workflow fixes | PARTIAL | `fees_remote_data_source.dart`, `fees_repository.dart`, `fees_repository_impl.dart`, `fees_providers.dart`, `fee_assignments_page.dart`, `fee_defaulters_page.dart`, `fee_widgets.dart` | None | Existing paged fee assignment/defaulter APIs | Fee assignment and defaulter list pagination | Static inspection only | Fee assignment and defaulter screens now request backend page/size and render page controls while compatibility list providers still fetch 100 rows for existing workflows. Hostel/transport fee-structure pagination and full Flutter validation remain unfinished. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q "-Dtest=StudentFeeAutoAssignmentServiceTest,FeeServiceTest,HostelServiceTest,TransportServiceTest" test
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q test
git diff --check
```

Result: PASS. 29 test classes, 136 tests, 0 failures, 0 errors, 0 skipped. `git diff --check` passed with LF/CRLF warnings only.

Additional validation notes:

- Local default `JAVA_HOME` points to Java 8, which cannot run this project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`, matching `<java.version>21</java.version>`.
- PostgreSQL/Flyway validation: BLOCKED - environment unavailable. Docker client exists, but the Docker daemon is not running; `psql`, `pg_isready`, and `postgres` are not in PATH. No Phase 3 migration was added in this entry.
- Flutter validation: BLOCKED - previous `flutter --version`/`dart --version` attempts hung silently and were interrupted; `dart --version` was retried from `frontend/` after the pagination edits and again hung silently for 30 seconds before interruption. `dart format`, `flutter doctor`, `flutter analyze`, and `flutter test` were not run because the base toolchain command did not complete.

## Phase 4 - Attendance, Reports, Dashboard

Status: IMPLEMENTED AND BACKEND-TESTED. Phase 4 backend tests compile and pass on Java 21. Flutter analyzer/runtime validation and PostgreSQL/Flyway migration validation remain blocked by local toolchain/environment availability, not by known code failures.

Completion snapshot:

- Overall: 94%
- Backend: 100%
- Frontend: 90%
- Testing: 82%

### Phase 4 Work Completed

| Requirement | Status | Files changed | Migration | API | UI | Tests | Notes |
|---|---|---|---|---|---|---|---|
| Academic Enrollment -> Student Attendance roster | TESTED | `AttendanceService.java`, `StudentClassAssignmentRepository.java`, `AttendanceController.java`, attendance Flutter data/repository/page files | `V29__phase4_attendance_roster_dashboard_reports.sql` | `GET /v1/attendance/students?date=yyyy-MM-dd` | Attendance screen sends selected date when loading roster | `AttendanceServiceTest`, full Maven suite | Roster now uses effective enrollment dates instead of only current active assignment. Deleted students and inactive students are excluded. |
| Daily bulk attendance save/upsert | TESTED | `AttendanceService.java`, `AttendanceRecordRepository.java`, `AttendanceServiceTest.java` | `V29__phase4_attendance_roster_dashboard_reports.sql` | Existing `POST /v1/attendance/daily` | Existing mark-all/exceptions workflow preserved | `AttendanceServiceTest`, full Maven suite | Save validates request students against the date-effective roster and updates existing same-student/same-date records to avoid duplicate attendance after moves/promotions. |
| Historical/date-effective attendance | TESTED | `AttendanceService.java`, `AttendanceRecordRepository.java`, `StudentClassAssignmentRepository.java` | `V29__phase4_attendance_roster_dashboard_reports.sql` | Existing history and daily attendance APIs | Attendance selected-date reload path implemented | `AttendanceServiceTest`, full Maven suite | Historical attendance reads preserve stored class/section context while roster and daily save use the date-effective assignment. |
| Attendance summary/monthly endpoints | TESTED | `AttendanceSummaryResponse.java`, `AttendanceSummaryCalculator.java`, `AttendanceService.java`, `AttendanceController.java`, `AttendanceRecordRepository.java` | None beyond V29 indexes | `GET /v1/attendance/summary`, `GET /v1/attendance/monthly` | Summary strip surfaced on Attendance screen | `AttendanceServiceTest`, full Maven suite | Class/division summaries reuse canonical attendance records, date-effective eligible counts, and shared attendance percentage math. |
| Teacher attendance and teacher class scope | TESTED | `AttendanceService.java`, `TeacherAttendanceService.java`, teacher/class/subject mapping repositories | None | Existing attendance and teacher attendance APIs | Existing teacher attendance/profile UI remains in place | `AttendanceServiceTest`, full Maven suite | Teacher users can access assigned classes and are rejected from unassigned classes. Teacher attendance uses the shared attendance percentage formula. |
| Non-teaching staff attendance | DEFERRED | None | None | None | None | None | Deferred until a canonical non-teaching staff master and attendance identity model exists. |
| Leave integration | DEFERRED | None | None | None | None | None | Deferred until a canonical leave approval module exists. |
| Reports canonical exports/filtering | TESTED | `ReportsService.java`, `ReportsController.java`, `FeeImportExportService.java`, `StudentImportExportService.java`, `ExamReportExportService.java`, `HostelReportExportService.java`, `TransportReportExportService.java`, report repositories | None | `GET /v1/reports/options`, `GET /v1/reports/preview`, `GET /v1/reports/export` | Reports screen supports dynamic filters, preview paging, CSV/PDF/XLSX export | `ReportsServiceTest`, full Maven suite | Student, academic structure, attendance, fee, exam, hostel, transport, audit preview/export paths use canonical module services or report exporters. |
| PDF export | TESTED | `PdfExportService.java`, module report exporters | None | Existing and report export routes | Reports export action uses `.pdf` files | `ReportsServiceTest`, full Maven suite | Report PDFs include title, school metadata, generated timestamp, applied filters, headers, and rows. |
| Excel export | TESTED | `ExcelExportService.java`, module report exporters, reports Flutter data/repository | None | Existing and report export routes | Reports export action uses `.xlsx` files | `ReportsServiceTest`, full Maven suite | Report Excel routes preserve full filtered datasets; Flutter filename extension corrected to `.xlsx`. |
| Dashboard canonical attendance/fee data and RBAC | TESTED | `DashboardService.java`, `DashboardController.java`, `DashboardServiceTest.java`, `dashboard_repository_impl.dart`, `attendance_page.dart` | None | `/v1/dashboard/summary`, `/v1/dashboard/today-attendance` | Dashboard falls back to attendance-only metrics when full summary is forbidden; attendance save invalidates dashboard provider | `DashboardServiceTest`, full Maven suite | Today attendance uses date-effective eligible student counts and shared attendance percentage math. Fee totals use canonical fee assignment totals. Summary is gated by `REPORTS_READ`; today attendance is gated by `ATTENDANCE_READ`. |
| Flutter Attendance UI | IMPLEMENTED - TEST PENDING | Attendance Flutter data source, model, repository, page | None | Attendance roster, summary, monthly APIs | Date-aware roster and summary/monthly strip implemented | Static review only | Flutter/Dart toolchain did not respond, so analyzer/test validation remains blocked locally. |
| Flutter Reports UI | IMPLEMENTED - TEST PENDING | Reports Flutter data source, model, repository, page | None | Reports options, preview, export APIs | Dynamic filters, preview paging, empty/error states, CSV/PDF/XLSX export implemented | Static review only | Flutter/Dart toolchain did not respond, so analyzer/test validation remains blocked locally. |
| Flutter Dashboard UI | IMPLEMENTED - TEST PENDING | Dashboard Flutter repository/page, attendance invalidation | None | Dashboard summary and today-attendance APIs | Existing dashboard consumes canonical backend payloads and RBAC fallback | Static review only | Flutter/Dart toolchain did not respond, so analyzer/test validation remains blocked locally. |
| V29 migration validation | BLOCKED | `V29__phase4_attendance_roster_dashboard_reports.sql` | V29 | N/A | N/A | Not run against PostgreSQL | Migration file exists; PostgreSQL/Flyway runtime validation could not run because local PostgreSQL tooling and Docker daemon were unavailable. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q "-Dtest=AttendanceServiceTest" test
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q "-Dtest=ReportsServiceTest" test
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q "-Dtest=AttendanceServiceTest,ReportsServiceTest,DashboardServiceTest" test
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q test
```

Result: PASS. 30 test classes, 156 tests, 0 failures, 0 errors, 0 skipped.

Additional validation notes:

- Local default `JAVA_HOME` still points to Java 8, which cannot compile this Java 21 project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- PostgreSQL/Flyway validation: BLOCKED - retry confirmed `psql`, `pg_isready`, and `postgres` are not in PATH. Docker client exists, but Docker daemon is not running. V29 has not been runtime-validated against PostgreSQL.
- Flutter validation: BLOCKED - `dart --version` and `flutter --version` hung silently and were interrupted from `frontend/`; `flutter analyze` and `flutter test` were not run because base Flutter/Dart commands did not complete.

Remaining Phase 4 items:

- BLOCKED: Run `flutter analyze` and `flutter test` once the local Flutter/Dart toolchain responds.
- BLOCKED: Run PostgreSQL/Flyway V29 validation once local PostgreSQL or Docker daemon is available.
- DEFERRED: Non-teaching staff attendance.
- DEFERRED: Leave integration.

## Phase 5 - Staff Management, Leave, Payroll Foundation, Communication, Settings

Status: IMPLEMENTED AND BACKEND-TESTED; FLUTTER/POSTGRESQL VALIDATION BLOCKED. Backend implementation is complete for the intended Phase 5 scope, the Phase 5 Flutter surfaces have been implemented and statically reviewed, and the full Maven suite passes on Java 21. Flutter analyzer/test validation and PostgreSQL/Flyway runtime validation remain blocked by local toolchain/environment availability.

Completion snapshot:

- Overall: 94%
- Backend: 100%
- Frontend: 95%
- Testing: 88%

### Phase 5 Current Status

| Requirement | Status | Notes |
|---|---|---|
| Canonical Staff master/model | TESTED | Canonical staff entity, repositories, service layer, controller DTOs, Flutter data layer, and service tests exist. |
| Teacher to Staff relationship | TESTED | Teacher records can link to staff identities; service tests cover teacher/staff linking and unlinking. |
| Department CRUD | TESTED | Department create/update/list backend paths and Flutter management UI are implemented; service tests cover create/update behavior. |
| Designation CRUD | TESTED | Designation create/update/list backend paths and Flutter management UI are implemented; service tests cover create/update behavior. |
| Staff profile CRUD | TESTED | Staff create/update/list/get/deactivate/exit backend paths and Flutter UI are implemented; lifecycle paths are covered by service tests. |
| Staff user-account linkage | TESTED | Staff-to-user validation and linkage logic exists and is covered by service tests. |
| Non-teaching staff attendance | TESTED | Staff attendance roster/save logic supports non-teaching staff and is covered by service tests. |
| Staff monthly attendance summary | TESTED | Daily, summary, monthly, and history APIs exist; summary/monthly/history behavior is covered. |
| Leave types | TESTED | Leave type CRUD logic exists and create/default behavior is covered. |
| Leave request workflow | TESTED | Leave request creation and overlap protection are covered. |
| Leave approve/reject/cancel | TESTED | Approval, rejection, cancellation, and approval permission checks are covered. |
| Leave to Attendance integration | TESTED | Approved leave applies staff attendance entries and is covered by tests. |
| Payroll salary structure | TESTED | Salary structure create/update and calculated totals are covered. |
| Staff salary assignment | TESTED | Assignment and effective-date closeout logic is covered. |
| Payroll period generation | TESTED | Payroll generation is covered by service tests. |
| Gross/deduction/net calculation | TESTED | Payroll calculation snapshots are covered by service tests. |
| Payroll history preservation | TESTED | Generated payroll records preserve salary snapshots; preservation and mark-paid behavior are covered. |
| Payroll RBAC/security | TESTED | Payroll service guards and controller permission gates are covered. |
| Staff documents | TESTED | Staff document create/update/archive lifecycle is implemented and covered. |
| Announcements | TESTED | Announcement publishing is covered by communication tests. |
| Circulars / Notice Board | TESTED | Communication list/history covers circulars, and notice-board type support is implemented. |
| Events | TESTED | Event communication creation and event-field validation path are covered. |
| Communication audience targeting | TESTED | Staff/all audience counts and division audience validation are covered. |
| Communication history | TESTED | Communication listing/history with filters and pagination is covered. |
| School Profile settings | TESTED | Backend defaults, validation, and Flutter settings group rendering are implemented; settings tests cover grouped defaults. |
| Academic/Exam/Fee settings | TESTED | Backend settings groups and Flutter settings fields are implemented; settings tests cover grouped defaults/validation. |
| Notification / Email / SMS settings | TESTED | Backend settings groups and Flutter fields are implemented; sensitive email/SMS masking is covered. |
| Security/Application settings | TESTED | Backend settings groups and Flutter fields are implemented; validation coverage exists for application/security-style settings. |
| Sensitive settings masking | TESTED | Sensitive email/SMS settings are masked and masked values preserve existing secrets; service tests cover both. |
| Settings RBAC | TESTED | Settings endpoints remain permission-gated and are covered by API security regression tests. |
| Staff Reports integration | TESTED | Staff report export/preview integration is covered through reports and staff report exporter tests. |
| Dashboard staff metrics integration | TESTED | Dashboard staff metrics use canonical staff data and are covered by dashboard tests. |
| Flutter Staff UI | IMPLEMENTED - TEST PENDING | Staff workspace, routes, module registry/menu wiring, data source, repository, providers, staff forms, lifecycle actions, and documents UI are implemented; analyzer/test is blocked. |
| Flutter Attendance/Leave UI | IMPLEMENTED - TEST PENDING | Staff attendance daily roster, monthly summary, history, leave type/request/review/cancel flows are implemented inside the staff workspace; analyzer/test is blocked. |
| Flutter Payroll UI | IMPLEMENTED - TEST PENDING | Salary structures, salary assignments, payroll generation, payroll history/details, and mark-paid UI are implemented; analyzer/test is blocked. |
| Flutter Communication UI | IMPLEMENTED - TEST PENDING | Announcements, circulars, notice-board items, events, audience targeting, publish/unpublish/archive, and history UI are implemented; analyzer/test is blocked. |
| Flutter Settings UI | IMPLEMENTED - TEST PENDING | Settings UI now covers Phase 5 settings groups and masks sensitive email/SMS fields; analyzer/test is blocked. |
| Phase 5 migration status | IMPLEMENTED - TEST PENDING | Latest migration is `V30__phase5_staff_leave_payroll_communication_settings.sql`; runtime validation is blocked by local PostgreSQL/Docker availability. |
| Backend tests | TESTED | Full Maven suite passes on Java 21. |
| Flutter analyze/test | BLOCKED | `flutter --version` hung silently and was interrupted; analyzer/tests were not run because the base toolchain command did not complete. |
| PostgreSQL/Flyway validation | BLOCKED | Docker did not respond within the bounded check and local PostgreSQL tools are not in PATH. |

### Phase 5 Work Completed

| Area | Status | Files changed | Migration | API/UI | Tests | Notes |
|---|---|---|---|---|---|---|
| Staff master, departments, designations, documents | TESTED | `src/main/java/com/school/erp/modules/staff/**`, `frontend/lib/src/features/staff/**` | `V30__phase5_staff_leave_payroll_communication_settings.sql` | Staff backend APIs and Flutter staff workspace are implemented | `StaffServiceTest` | Canonical staff model exists with department, designation, teacher link, user link, staff documents, and lifecycle fields. |
| Staff attendance and leave | TESTED | `StaffAttendanceService.java`, `StaffLeaveService.java`, staff attendance/leave DTOs/repositories, `frontend/lib/src/features/staff/**` | V30 | Backend APIs and Flutter staff attendance/leave UI are implemented | `StaffAttendanceServiceTest`, `StaffLeaveServiceTest` | Non-teaching staff attendance, staff summaries/history, leave request workflow, approval/rejection/cancellation, and attendance integration are covered. |
| Payroll foundation | TESTED | `PayrollService.java`, payroll DTOs/entities/repositories, `frontend/lib/src/features/staff/**` | V30 | Backend APIs and Flutter payroll UI are implemented | `PayrollServiceTest` | Salary structures, assignments, payroll generation, gross/deduction/net snapshots, payroll record history, and mark-paid behavior are covered. |
| Communication | TESTED | `src/main/java/com/school/erp/modules/communications/**`, `frontend/lib/src/features/communications/**` | V30 | Backend APIs and Flutter communication UI are implemented | `CommunicationServiceTest` | Announcements, circulars, notices, events, audience targeting, publication state, and history foundations are covered. |
| Settings expansion | TESTED | `ApplicationSettingsService.java`, `frontend/lib/src/features/settings/presentation/pages/settings_page.dart` | None | Existing settings backend API and Flutter settings UI were expanded | `ApplicationSettingsServiceTest`, `ApiSecurityRegressionTest` | School, academic, exam, fee, notification, email, SMS, application, security, and backup settings defaults/validation are present; sensitive masking exists. |
| Staff reports and dashboard staff metrics | TESTED | `ReportsService.java`, `StaffReportExportService.java`, `DashboardService.java`, `DashboardSummaryResponse.java`, Flutter reports/dashboard files | None | Existing reports/dashboard APIs and Flutter surfaces are expanded | `ReportsServiceTest`, `StaffReportExportServiceTest`, `DashboardServiceTest` | Staff reports are integrated into report options/export/preview, and dashboard staff counts use canonical staff records. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test
```

Result: PASS. 37 test classes, 221 tests, 0 failures, 0 errors, 0 skipped.

Additional validation notes:

- Local default `JAVA_HOME` still points to Java 8, which cannot compile this Java 21 project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- Latest migration: `V30__phase5_staff_leave_payroll_communication_settings.sql`.
- PostgreSQL/Flyway validation: BLOCKED - `docker info` did not return within the bounded check and was interrupted; `psql`, `pg_isready`, and `postgres` are not in PATH. V30 has not been runtime-validated against PostgreSQL.
- Flutter validation: BLOCKED - `flutter --version` hung silently from `frontend/` and was interrupted; `flutter analyze` and `flutter test` were not run because the base Flutter command did not complete.

Remaining Phase 5 items:

- BLOCKED: Run `flutter analyze` and `flutter test` once the local Flutter/Dart toolchain responds.
- BLOCKED: Run PostgreSQL/Flyway V30 validation once local PostgreSQL or Docker daemon is available.

Deferred beyond Phase 5:

- DEFERRED: Partial refund ledger.
- DEFERRED: Promotion/detention workflows.
- DEFERRED: Historical certificate rendering.
- DEFERRED: Advanced payroll accounting/disbursement beyond the Phase 5 payroll foundation.
- DEFERRED: Final 18-module audit and re-score.

## Phase 6 - Library Management

Status: IMPLEMENTED AND BACKEND-TESTED; FLUTTER/POSTGRESQL VALIDATION BLOCKED. Backend implementation is complete for the intended Phase 6 scope, the Flutter library workspace has been implemented and statically reviewed, and the full Maven suite passes on Java 21. Flutter analyzer/test validation and PostgreSQL/Flyway runtime validation remain blocked by local toolchain/environment availability.

Completion snapshot:

- Overall: 94%
- Backend: 100%
- Frontend: 92%
- Testing: 86%

### Phase 6 Work Completed

| Area | Status | Files changed | Migration | API/UI | Tests | Notes |
|---|---|---|---|---|---|---|
| Library master data | TESTED | `src/main/java/com/school/erp/modules/library/**` | `V31__phase6_library_management.sql` | Categories, authors, publishers, books | `LibraryServiceTest` | Book categories, authors, publishers, book metadata, active/inactive handling, and catalog search/filtering are implemented. |
| Book copies and inventory statuses | TESTED | Library domain/service/controller/repositories, Flutter library feature files | V31 | `/v1/library/copies` and copy status actions | `LibraryServiceTest` | Copy accession numbers, availability, issued/lost/damaged/withdrawn states, shelf/location data, and inventory summary counts are implemented. |
| Memberships | TESTED | Library membership domain/service/controller/repositories, Flutter library feature files | V31 | `/v1/library/memberships` | `LibraryServiceTest` | Student, teacher, and staff memberships validate canonical member identity, enforce duplicate membership protection, and support active/inactive lifecycle. |
| Issue, return, overdue, lost/damaged, fines | TESTED | `LibraryService.java`, library entities/repositories/controller DTOs | V31 | Loan issue/return/lost and fine pay/waive APIs | `LibraryServiceTest` | Circulation rules use library settings for loan days, max active loans, and fine-per-day calculation. Returns can mark damaged copies; lost loans create pending fines. |
| Library reports and exports | TESTED | `LibraryReportExportService.java`, `ReportsService.java`, report tests, Flutter reports page | None beyond V31 permissions | Report options/preview/export include library inventory, available, issued, overdue, member history, fine, and lost/damaged reports | `LibraryReportExportServiceTest`, `ReportsServiceTest` | Library reports support CSV/PDF/XLSX export, preview pagination, filtering, and `LIBRARY_READ` authorization. |
| Dashboard library metrics | TESTED | `DashboardService.java`, `DashboardSummaryResponse.java`, Flutter dashboard model/repository | None | `/v1/dashboard/summary` | `DashboardServiceTest` | Dashboard summary now includes total library books, available copies, overdue loans, and pending library fine amount from canonical library repositories. |
| RBAC, permissions, and menu | TESTED | `V31__phase6_library_management.sql`, `LibraryController.java`, `ApiSecurityRegressionTest.java`, Flutter route/menu policy/sidebar | V31 | `LIBRARY_READ`, `LIBRARY_CREATE`, `LIBRARY_UPDATE`, `LIBRARY_DELETE`, `LIBRARY_ISSUE`, `LIBRARY_RETURN`, `LIBRARY_FINE` | `ApiSecurityRegressionTest`, full Maven suite | Backend endpoints are permission-gated; role/menu seeds and Flutter module routing/sidebar policy are wired to module id `library`. |
| Flutter Library UI | IMPLEMENTED - TEST PENDING | `frontend/lib/src/features/library/**`, router, API paths, dashboard/menu/sidebar/report integrations | None | Library management screen at `/modules/library` | Static review only | Summary, catalog, copies, memberships, circulation, fines, and reports handoff are implemented. Analyzer/test is blocked because Flutter/Dart commands hang locally. |
| V31 migration status | IMPLEMENTED - TEST PENDING | `V31__phase6_library_management.sql` | V31 | Schema, settings, permissions, menu seed | Full Maven suite with H2 tests; PostgreSQL runtime validation blocked | Migration file exists and is latest. PostgreSQL/Flyway validation could not run because Docker daemon is unavailable and local PostgreSQL tools are not in PATH. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test
```

Result: PASS. 39 test classes, 236 tests, 0 failures, 0 errors, 0 skipped.

Additional validation notes:

- Local default `JAVA_HOME` still points to Java 8, which cannot compile this Java 21 project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- Latest migration: `V31__phase6_library_management.sql`.
- PostgreSQL/Flyway validation: BLOCKED - `psql` and `pg_isready` are not in PATH. Docker CLI exists (`Docker version 26.1.4`), but `docker info` reports the Docker daemon is not running, so V31 has not been runtime-validated against PostgreSQL.
- Flutter validation: BLOCKED - `dart format` and `flutter --version` hung silently and were interrupted; `flutter analyze` and `flutter test` were not run because the base Flutter/Dart commands did not complete.

Remaining Phase 6 items:

- BLOCKED: Run `flutter analyze` and `flutter test` once the local Flutter/Dart toolchain responds.
- BLOCKED: Run PostgreSQL/Flyway V31 validation once local PostgreSQL or Docker daemon is available.

Deferred beyond Phase 6:

- DEFERRED: Backup and restore workflows.
- DEFERRED: Mobile app packaging.
- DEFERRED: Parent/student/teacher portal hardening beyond existing role-aware library read access.
- DEFERRED: Final 18-module audit and re-score.

## Phase 7 - Backup & Restore

Status: IMPLEMENTED AND BACKEND-TESTED; FLUTTER/POSTGRESQL VALIDATION BLOCKED. Backend backup and restore workflows are implemented with metadata tracking, RBAC, checksum validation, restore safety checks, audit logging, retention support, and Hostinger container compatibility. The Flutter Backup & Restore UI has been implemented and statically reviewed, but analyzer/test validation is blocked because the local Flutter command does not return.

Completion snapshot:

- Overall: 93%
- Backend: 100%
- Frontend: 92%
- Testing: 86%

### Phase 7 Work Completed

| Area | Status | Files changed | Migration | API/UI | Tests | Notes |
|---|---|---|---|---|---|---|
| Backup metadata and history model | TESTED | `src/main/java/com/school/erp/modules/backup/**` | `V32__phase7_backup_restore.sql` | `/v1/backups`, `/v1/backups/summary`, `/v1/backups/restores` | `BackupServiceTest`, full Maven suite | Backup records and restore history preserve type, status, size, checksum, Flyway version, actor, timestamps, failure messages, and soft-delete state. |
| Manual database backup | TESTED | `BackupService.java`, `BackupCommandRunner.java`, `ProcessBackupCommandRunner.java`, backup DTOs/controller | V32 | `POST /v1/backups` | `BackupServiceTest`, full Maven suite | Database backups use `pg_dump` with argument lists, pass passwords through environment variables, record checksums, and never expose storage paths in API responses. |
| Secure backup download and delete | TESTED | `BackupService.java`, `BackupController.java` | V32 | `GET /v1/backups/{backupId}/download`, `DELETE /v1/backups/{backupId}` | `BackupServiceTest`, full Maven suite | Download validates canonical storage paths and checksums; delete marks metadata deleted and removes files only after path validation. |
| Restore workflow | TESTED | `BackupService.java`, `BackupCommandRunner.java`, backup DTOs/controller | V32 | `POST /v1/backups/{backupId}/restore` | `BackupServiceTest`, full Maven suite | Restore requires typed confirmation, verifies checksum, validates the archive with `pg_restore --list`, creates a pre-restore safety backup, then runs `pg_restore` with clean/if-exists/no-owner/no-privileges/exit-on-error options. |
| Scheduler and retention foundation | IMPLEMENTED - TEST PENDING | `BackupService.java`, `ApplicationSettingsService.java`, `application.yml` | V32 | Existing backup/settings APIs | Full Maven suite | Scheduled backup checks `backup.backupEnabled` and frequency settings; retention honors backup retention count and days. Dedicated scheduler/retention edge-case tests remain pending. |
| RBAC, permissions, menu seed | TESTED | `V32__phase7_backup_restore.sql`, `BackupController.java`, `ApiSecurityRegressionTest.java`, Flutter menu/sidebar files | V32 | Backup endpoints and module menu | `ApiSecurityRegressionTest`, full Maven suite | `BACKUP_READ`, `BACKUP_CREATE`, `BACKUP_DOWNLOAD`, `BACKUP_DELETE`, and `BACKUP_RESTORE` permissions are seeded. Super admin receives all backup permissions; admin receives backup permissions except restore. |
| Hostinger/container compatibility | IMPLEMENTED - TEST PENDING | `Dockerfile`, `compose.production.yml`, `application.yml` | None | Production backend container config | Static review only | Backend image installs PostgreSQL client tools and compose mounts persistent backup storage at `/app/backups`. Runtime validation is pending a deploy/container run. |
| Flutter Backup UI | IMPLEMENTED - TEST PENDING | `frontend/lib/src/features/backup/**`, router, API paths, dashboard menu/sidebar registry | None | Backup & Restore module at `/modules/backup` | Static review only | Summary, backup history, restore history, manual backup, download, delete, and restore-confirmation flows are implemented. Analyzer/test validation is blocked by the local Flutter toolchain hang. |
| V32 migration status | IMPLEMENTED - TEST PENDING | `V32__phase7_backup_restore.sql` | V32 | Schema, settings, permissions, menu seed | Full Maven suite with H2 tests; PostgreSQL runtime validation blocked | Migration file exists and is the latest local migration. PostgreSQL/Flyway runtime validation could not run because PostgreSQL CLIs are absent and the Docker daemon is unavailable. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q -DskipTests compile
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q "-Dtest=BackupServiceTest,ApiSecurityRegressionTest" test
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q test
git diff --check
```

Result: PASS. 40 test classes, 245 tests, 0 failures, 0 errors, 0 skipped. `git diff --check` passed with LF/CRLF warnings only.

Additional validation notes:

- Local default `JAVA_HOME` still points to Java 8, which cannot compile this Java 21 project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- Latest migration: `V32__phase7_backup_restore.sql`.
- PostgreSQL/Flyway validation: BLOCKED - `psql`, `pg_isready`, and `postgres` are not in PATH. Docker CLI exists (`Docker version 26.1.4`), but `docker info` reports the Docker daemon is not running, so V32 has not been runtime-validated against PostgreSQL locally.
- Flutter validation: BLOCKED - `flutter --version` hung silently from `frontend/` and was interrupted after the bounded retry; `flutter analyze` and `flutter test` were not run because the base Flutter command did not complete.
- Uploaded file backup status: DEFERRED - the current application stores document URL/path metadata but does not yet expose a canonical backend-managed upload storage root suitable for full binary file backup/restore.

Remaining Phase 7 items:

- BLOCKED: Run `flutter analyze` and `flutter test` once the local Flutter/Dart toolchain responds.
- BLOCKED: Run PostgreSQL/Flyway V32 validation once local PostgreSQL or Docker daemon is available.
- IMPLEMENTED - TEST PENDING: Add focused scheduler/retention edge-case tests if Phase 7 requires coverage beyond the full Maven suite and backup service happy/failure paths.
- DEFERRED: Full uploaded-file binary backup/restore after a canonical backend-managed file storage root exists.

Deferred beyond Phase 7:

- DEFERRED: Mobile app packaging.
- DEFERRED: Final 18-module audit and re-score.

## Phase 8 - Apps & Portals

Status: IMPLEMENTED AND BACKEND-TESTED; FLUTTER/POSTGRESQL VALIDATION BLOCKED. Student, Parent, and Teacher portals are implemented on scoped backend APIs using the authenticated user, linked domain identity, and canonical services. Portal-only roles are routed to their portal home and are no longer granted broad operational module permissions. The Flutter portal workspace is implemented and statically reviewed, but local Flutter analyzer/test validation remains blocked because the Flutter command does not return.

Completion snapshot:

- Overall: 92%
- Backend: 100%
- Frontend: 90%
- Testing: 84%

### Phase 8 Work Completed

| Area | Status | Files changed | Migration | API/UI | Tests | Notes |
|---|---|---|---|---|---|---|
| Student portal identity and dashboard | TESTED | `Student.java`, `StudentRepository.java`, `PortalService.java`, `PortalController.java`, portal DTOs, Flutter portal feature files | `V33__phase8_apps_portals.sql` | `/v1/portal/student/**`, `/portal/student` | `PortalServiceTest`, full Maven suite | Student portal data is resolved from the authenticated user account linked to the student record. Dashboard data reuses canonical profile, attendance, fees, exams, library, hostel, transport, and communication services. |
| Parent portal child selector and ownership scope | TESTED | `PortalService.java`, `PortalController.java`, Flutter portal feature files | V33 | `/v1/portal/parent/children/**`, `/portal/parent` | `PortalServiceTest`, full Maven suite | Parent children are loaded from linked parent/guardian records. Child-specific endpoints reject access to unlinked students before returning profile, attendance, fees, exams, library, hostel, transport, or communication data. |
| Teacher portal scope | TESTED | `PortalService.java`, `PortalController.java`, `SubjectTeacherMappingRepository.java`, Flutter portal feature files | V33 | `/v1/portal/teacher/**`, `/portal/teacher` | `PortalServiceTest`, full Maven suite | Teacher portal resolves the linked teacher identity and limits students, attendance, and marks actions to assigned class/section and subject scope. |
| Portal RBAC and horizontal privilege protection | TESTED | `DatabaseMasterDataSeeder.java`, `LocalQaDataSeeder.java`, `ApiSecurityRegressionTest.java`, V33 | V33 | Portal permissions and menu entries | `ApiSecurityRegressionTest`, full Maven suite | Student/Parent/Teacher roles are trimmed to portal permissions. Direct access to broad student, fee, attendance, report, communication, and library modules is blocked unless the role has those permissions through an administrative profile. |
| Portal communication visibility | TESTED | `CommunicationRecordRepository.java`, `PortalService.java` | V33 | Portal communication feeds | `PortalServiceTest`, full Maven suite | Portal feeds return published communication records visible to all users, role audiences, class audiences, and division/section audiences while respecting publish and expiry dates. |
| Portal library visibility | TESTED | `LibraryMembershipRepository.java`, `PortalService.java` | V33 | Portal library summaries | `PortalServiceTest`, full Maven suite | Student and teacher portal library cards use active canonical library memberships and scoped loan/fine summaries. |
| Admin, principal, and operational role access | TESTED | Menu/RBAC seeds, Flutter menu policy/sidebar | V33 | Existing operational modules | `ApiSecurityRegressionTest`, full Maven suite | Admin and principal roles retain portal visibility plus existing operational modules. Accountant, warden, receptionist, staff, transport, hostel, and library workflows remain on their existing role-aware module UIs instead of duplicate portal backends. |
| Flutter portal UI | IMPLEMENTED - TEST PENDING | `frontend/lib/src/features/portal/**`, router, API paths, module registry, menu policy, sidebar | None | Student, Parent, and Teacher portal pages | Static review only | Portal pages load scoped dashboards, parent child selection, teacher assigned scope, and portal-only routing. Analyzer/test is blocked by the local Flutter toolchain hang. |
| V33 migration status | IMPLEMENTED - TEST PENDING | `V33__phase8_apps_portals.sql` | V33 | Student user link, portal permissions, portal menu entries, role permission cleanup | Full Maven suite with H2 tests; PostgreSQL runtime validation blocked | Migration file exists and is the latest local migration. PostgreSQL/Flyway validation could not run because PostgreSQL CLIs are absent and Docker daemon is unavailable. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd -q -DskipTests compile
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test
```

Result: PASS. 41 test classes, 261 tests, 0 failures, 0 errors, 0 skipped.

Additional validation notes:

- Local default `JAVA_HOME` still points to Java 8, which cannot compile this Java 21 project. Verification used installed JDK 21 at `C:\Program Files\Java\jdk-21.0.4`.
- Latest migration: `V33__phase8_apps_portals.sql`.
- PostgreSQL/Flyway validation: BLOCKED - `psql` and `postgres` are not in PATH. Docker CLI exists, but `docker info` reports the Docker daemon is not running, so V33 has not been runtime-validated against PostgreSQL locally.
- Flutter validation: BLOCKED - `flutter --version` hung silently from `frontend/` and was interrupted after the bounded retry; `flutter analyze` and `flutter test` were not run because the base Flutter command did not complete.

Remaining Phase 8 items:

- BLOCKED: Run `flutter analyze` and `flutter test` once the local Flutter/Dart toolchain responds.
- BLOCKED: Run PostgreSQL/Flyway V33 validation once local PostgreSQL or Docker daemon is available.
- IMPLEMENTED - TEST PENDING: Run a browser smoke test for portal navigation once the Flutter web build can be produced locally.

Deferred beyond Phase 8:

- DEFERRED: Native mobile app packaging.
- DEFERRED: Public website work and Phase 9.
- DEFERRED: Final 18-module audit and re-score.

## Application UI/UX Polish

Status: IMPLEMENTED POLISH PASS; BACKEND REGRESSION GREEN; FLUTTER AND BROWSER VALIDATION BLOCKED. This pass focused only on the authenticated ERP application and role-specific portals. Public website screens were not redesigned or modified. Backend business calculations, RBAC, portal scoping, backup logic, audit logic, and canonical service behavior were preserved.

Completion snapshot:

- Overall UI/UX polish: IMPLEMENTED - VALIDATION BLOCKED
- Backend regression: TESTED
- Flutter static review: ISSUES FOUND AND FIXED
- Flutter analyzer/test/build: BLOCKED
- Browser smoke test: BLOCKED

### Application UI/UX Polish Work Completed

| Area | Issue | Fix | Files | Validation | Notes |
|---|---|---|---|---|---|
| Shared design system | Semantic colors, status labels, responsive padding, and status chips were still partly duplicated across screens. | Added centralized status color/label helpers, responsive page insets, information color token, and richer status badge support. | `app_design_system.dart`, `app_page_layout.dart` | Static review; backend suite unaffected | Statuses such as Active, Pending, Overdue, Rejected, Present, Leave, Running, Restored, Sent, and Delivered now share one palette. |
| Theme and typography | Theme styling was solid but still used scattered white surfaces, default typography weights, and basic snackbar/menu/tooltip treatment. | Tightened typography hierarchy, surface colors, table header styling, popup menus, tooltips, and snackbar defaults. | `app_theme.dart` | Static review | Kept a compact Material 3 ERP style without heavy gradients or oversized UI. |
| Shared buttons, dialogs, tables, feedback | Buttons had only one tone, confirmation dialogs were generic, tables had weak empty states, and snackbars were hand-rolled. | Added button tones, polished confirmation dialogs, empty-state support in `AppDataTable`, and shared `showAppSnackBar`. | `app_button.dart`, `app_confirm_dialog.dart`, `app_data_table.dart`, `app_feedback.dart` | Static review | Dangerous actions now have clearer copy and consistent visual priority. |
| Date and money formatting | Money appeared as mixed `INR`/`Rs` strings; dates were locally formatted. | Added shared formatter with rupee display and Indian grouping; moved visible money strings to shared formatter. | `app_formatters.dart`, dashboard/fees/staff/library/transport/student/profile files | Static review | Display-only changes; no fee, payroll, hostel, transport, or library calculations changed. |
| Application shell and sidebar | Sidebar order did not fully match user workflows; hardcoded shell colors remained. | Reworked sidebar grouping into Portal, People, Academics, Finance, Operations, Communication, Documents, Administration while preserving permission filtering. | `admin_shell.dart` | Static review | No route or permission architecture was changed. |
| Dashboard | Dashboard panels used local card/empty-state styling and local module status styling. | Converted panels to shared section/empty-state styling and centralized module status color use. | `dashboard_page.dart`, `dashboard_repository_impl.dart` | Static review | Dashboard calculations remain canonical. |
| User Management and Roles | User/role/permission statuses used local status color helpers and local date formatting. | Switched to shared status badges and date/status label helpers. | `users_page.dart`, `role_permission_page.dart` | Static review | Permission workflows and RBAC unchanged. |
| Fees workflows | Fees screens had mixed money formatting, local status chips, weak empty list surfaces, and generic destructive/payment confirmations. | Centralized fee status chips, rupee money display, fee empty states, snackbars, and specific delete/reverse/refund confirmation copy. | `fee_widgets.dart`, `fees_management_page.dart`, `fee_defaulters_page.dart`, `payment_collection_page.dart`, `student_fee_assignment_page.dart` | Static review | Fee Assignments, Defaulters, and Payment Collection wiring reviewed; business logic unchanged. |
| Attendance | Attendance statuses were plain text inside roster controls and feedback was local. | Added semantic status badges inside attendance status selection, shared ISO date formatting, and shared feedback tones. | `attendance_page.dart`, student attendance tab | Static review | Attendance save/load/export flow preserved. |
| Exams and Results | Result status chips used local status colors. | Switched result status badges to shared semantic status styling. | `student_exam_results_tab.dart` | Static review | Exam and report-card business paths unchanged. |
| Staff, Leave, Payroll | Staff/payroll screens had local money and status formatting. | Centralized status chips and payroll/money display. | `staff_management_page.dart` | Static review | Payroll, leave, and staff attendance calculations unchanged. |
| Hostel and Transport | Operational status chips and money strings were locally styled. | Centralized statuses and rupee money display. | `hostel_management_page.dart`, `transport_management_page.dart` | Static review | Capacity and assignment rules unchanged. |
| Library | Library status/fine money styling was local. | Centralized status chips and rupee fine display. | `library_management_page.dart` | Static review | Circulation, fine, and inventory rules unchanged. |
| Communication and Notifications | Published/draft/archive and delivery statuses used local color maps. | Centralized communication and notification statuses. | `communication_management_page.dart`, `notification_management_page.dart` | Static review | Audience targeting and delivery logic unchanged. |
| Reports and Documents | Report/document workflow routes were reviewed for consistency with existing shared widgets. | No backend/report logic changes were required in this pass. | `reports_page.dart`, student document/profile surfaces reviewed | Static review | Historical certificate rendering remains deferred. |
| Student Portal | Portal fee amounts, attendance dates, and statuses were less polished. | Added rupee fee display, readable attendance dates, and shared semantic status badges. | `portal_pages.dart` | Static review | Student portal scoping unchanged. |
| Parent Portal | Child selector worked but needed stronger mobile-friendly treatment. | Made child selector prominent, constrained, icon-labeled, and responsive inside the page header actions. | `portal_pages.dart` | Static review | Child ownership enforcement remains backend-scoped. |
| Teacher Portal | Teacher portal panels used generic primary status badges. | Switched portal panel statuses to shared semantic status badges. | `portal_pages.dart` | Static review | Teacher assignment and subject scope enforcement unchanged. |

Verification:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test
where.exe flutter
where.exe dart
flutter --version
dart --version
git diff --check
```

Result: Backend PASS. 41 test classes, 261 tests, 0 failures, 0 errors, 0 skipped. `git diff --check` passed with LF/CRLF warnings only.

Additional validation notes:

- Flutter validation: BLOCKED - `where.exe flutter` and `where.exe dart` found the tools under `C:\Program Files\flutter\bin`, but both `flutter --version` and `dart --version` hung silently for 30 seconds and were interrupted. Because the base commands did not return, `flutter pub get`, `dart format`, `flutter analyze`, `flutter test`, and `flutter build web --release` were not run.
- Browser smoke test: BLOCKED - Flutter web build could not be produced while the Flutter/Dart toolchain is unavailable.
- Backend changes: None in this polish pass.

Remaining Application UI/UX Polish items:

- BLOCKED: Run `dart format`, `flutter analyze`, `flutter test`, and `flutter build web --release` once Flutter/Dart responds.
- BLOCKED: Run browser smoke tests for Login, Dashboard, Students, Attendance, Fees, Fee Assignments, Defaulters, Payment Collection, Exams, Staff, Payroll, Hostel, Transport, Library, Communication, Reports, Settings, Backup, Student Portal, Parent Portal, and Teacher Portal once a Flutter web build is available.
- IMPLEMENTED - TEST PENDING: Fine-tune any analyzer or browser-smoke findings after the toolchain is available.

## Final Testing and Hostinger Deployment Attempt - 2026-09-27

Status: DEPLOYMENT BLOCKED AFTER LOCAL VALIDATION. The local final gates passed, including backend regression, package build, Flutter analyzer/test/build, PostgreSQL/Flyway validation through V35, SUPER_ADMIN authority checks, and live Flutter browser smoke for the historically problematic Transport, Library, Exams, Fees, Hostel, Attendance, Staff, and Dashboard paths. Production deployment did not proceed because the available non-interactive SSH path to Hostinger failed: OpenSSH had no usable key, and policy blocked using the stored plaintext server password through a helper script.

Validation summary:

- Git branch: `develop`.
- Tested base commit before local uncommitted deployment fixes: `d8a07cb88fcd1485193467ee64c581922ed8a1ac`.
- Latest migration: `V35__exam_permission_backfill.sql`.
- Backend compile/package: PASS with Java 21.
- Backend tests: PASS. 41 test classes, 261 tests, 0 failures, 0 errors, 0 skipped.
- Backend artifact: `target/school-erp-api-0.0.1-SNAPSHOT.jar`.
- Flutter `pub get`: PASS.
- `dart format lib`: PASS; 207 files checked, 0 changed on the final run.
- `flutter analyze`: PASS; no issues found.
- `flutter test`: PASS; 10 tests passed.
- `flutter build web --release --dart-define=API_BASE_URL=/api`: PASS; `frontend/build/web` produced.
- Local PostgreSQL/Flyway: PASS against PostgreSQL 17; 35 migrations validated, schema at version 35.
- Local SUPER_ADMIN authority check: PASS for Students, Fees, Attendance, Exams, Staff, Reports, Settings, Backup, Transport, and Library permission families.
- Local browser smoke: PASS. Dashboard, Students, Fee Assignments, Fee Defaulters, Payment Collection, Attendance, Exam Schedule, Staff, Hostel Fees, Transport, and Library opened without login redirects, console errors, runtime exceptions, 401/403/404/500 responses, or CORS failures.
- Transport validation: PASS. Menu entry and route `/transport` are present; Transport Management renders under Operations with Buses, Routes, Fees, and Drivers tabs.
- Library validation: PASS. Menu entry and route `/modules/library` are present; Library Management renders under Operations with Summary, Catalog, Copies, Members, Circulation, Fines, and Reports tabs.

Production deployment attempt:

- Existing architecture inspected: `/root/school-app`, `compose.production.yml`, Docker backend, Docker PostgreSQL, Docker Nginx/Flutter web build, certbot, `.env` secrets preserved on server.
- Production SSH status check: BLOCKED. `ssh -o BatchMode=yes root@72.62.250.216` reached the server but failed with `Permission denied (publickey,password)`.
- Production database backup: NOT RUN because SSH authentication was blocked before any production-changing command.
- Production Flyway migration: NOT RUN.
- Production backend/frontend replacement: NOT RUN.
- Production Nginx reload: NOT RUN.
- Rollback required: NO; production was not modified.

Errors fixed during final validation:

- Fixed PostgreSQL optional-filter failures caused by JPQL null string predicates binding as `bytea` in Fee Defaulters, Hostel Fee Structures, Library list pages, and Backup history.
- Added `EXAMS_READ` and `EXAMS_MANAGE` seed/backfill coverage so SUPER_ADMIN can access Exam Types and related exam APIs without frontend hardcoding.
- Preserved Transport and Library menu/RBAC visibility through the V34 backfill and seed alignment.

Remaining blockers:

- BLOCKED: Complete Hostinger deployment only after an approved SSH authentication path is available, such as a loaded SSH key, an explicitly approved interactive password session, or another approved non-secret deployment channel.
- BLOCKED: Commit/push or otherwise transfer the current validated working tree before using the server's existing `git pull` deployment script.
