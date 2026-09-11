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
