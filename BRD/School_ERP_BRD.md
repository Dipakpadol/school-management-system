# School ERP Business Requirement Document

## 1. Document Overview

| Item | Details |
| --- | --- |
| Document Name | School ERP Business Requirement Document |
| Version | 1.0 |
| Date | 26 May 2026 |
| Prepared For | School ERP Client Review |
| Prepared By | Business Analysis, Solution Architecture, and Full-Stack Engineering Team |
| System Type | Modular monolith School ERP platform |
| Technology Stack | Java 21, Spring Boot 3, PostgreSQL, Flyway, JWT, JPA/Hibernate, Flutter Admin Panel, Riverpod, Dio, GoRouter |

This BRD defines the business objectives, scope, functional requirements, non-functional requirements, module requirements, delivery phases, and acceptance criteria for the School ERP system. It is intended for management, client stakeholders, product owners, engineering, QA, and implementation teams.

## 2. Project Objective

The objective is to build a secure, scalable, and user-friendly School ERP platform that centralizes academic, administrative, financial, hostel, attendance, examination, notification, and reporting operations for schools.

The system must reduce manual work, improve data accuracy, provide real-time operational visibility, support role-based access, and create a foundation for future parent/student mobile applications and possible microservice separation.

## 3. Scope of the System

The School ERP will support the following business areas:

- Authentication, authorization, roles, and permissions.
- User, staff, student, and parent management.
- Academic setup including years, classes, sections, subjects, mappings, and timetable basics.
- Fee setup, assignment, collection, receipts, discounts, late fees, refunds, and reports.
- Hostel setup, room/bed management, allocation, transfer, vacating, and occupancy reporting.
- Attendance capture and reports for students and staff.
- Exams, marks entry, grade setup, result generation, and report cards.
- Notifications through template-based email/SMS/push-ready workflows.
- Reports and analytics across key modules.
- Import/export framework for Excel, CSV, and PDF.
- Audit logs for critical user and system actions.
- Admin web panel and future mobile app support.

Out of scope for MVP unless explicitly planned:

- Online payment gateway settlement reconciliation.
- Biometric attendance device integration.
- Live transport GPS tracking.
- Advanced timetable auto-generation.
- Full HR/payroll statutory compliance automation.

## 4. Stakeholders

| Stakeholder | Responsibilities |
| --- | --- |
| School Management | Business approval, reporting needs, operational oversight |
| Principal | Academic operations, student oversight, reports |
| Admin/Super Admin | System configuration, users, permissions, master data |
| Teachers | Attendance, academic records, exams, student progress |
| Accountant | Fee setup, fee collection, refunds, receipts, finance reports |
| Receptionist | Admissions, student enquiries, front-office updates |
| Warden | Hostel allocation, vacating, occupancy operations |
| Parents | Student profile, fees, attendance, results, notifications |
| Students | Profile, attendance, fee status, academic records |
| IT Support | Deployment, backup, monitoring, issue resolution |
| QA Team | Test planning, validation, acceptance certification |
| Development Team | Implementation, maintenance, integration |

## 5. User Roles

| Role | Business Purpose |
| --- | --- |
| SUPER_ADMIN | Full platform owner access and high-risk administrative operations |
| ADMIN | Institution administration, configuration, and user management |
| PRINCIPAL | Academic and operational leadership access |
| TEACHER | Student, attendance, academic, and exam workflows |
| ACCOUNTANT | Fee setup, payment collection, refunds, receipts, financial reports |
| RECEPTIONIST | Admission, front-office student/parent operations |
| STUDENT | Student self-service portal access |
| PARENT | Parent portal access for linked students |
| WARDEN | Hostel operations and occupancy management |

## 6. Functional Requirements

The system shall:

- Authenticate users securely using JWT.
- Enforce role-based and permission-based API access.
- Maintain audit fields and audit logs for critical operations.
- Manage students from admission through lifecycle status changes.
- Manage parents/guardians and map them to students.
- Manage academic setup and class/section/subject allocation.
- Manage fee categories, fee structures, assignments, payments, discounts, late fees, refunds, receipts, defaulters, and reports.
- Manage users, roles, and permissions.
- Provide responsive Flutter Admin Panel screens.
- Support import/export for operational data.
- Support PDF outputs where business documents are required.
- Support pagination, sorting, search, and filters on list APIs.
- Support soft delete for operational records and avoid hard delete for financial records.
- Provide reports and dashboard metrics from database records.

## 7. Non-Functional Requirements

| Area | Requirement |
| --- | --- |
| Security | JWT authentication, RBAC, permission checks, password hashing, CORS control, input validation |
| Performance | Common list APIs should support pagination and indexes for searchable fields |
| Scalability | Modular monolith package boundaries must allow future microservice separation |
| Availability | Backend should support health checks and environment-based configuration |
| Maintainability | Clean architecture, DTOs, mappers, services, repositories, controller layering |
| Data Integrity | UUID primary keys, validation, database constraints, Flyway migrations |
| Auditability | Audit fields and audit log records for critical actions |
| Usability | Responsive admin UI with loading, error, empty, and confirmation states |
| Compliance | Financial records must be traceable and not physically deleted |
| Observability | Structured logs, correlation ID, clear errors, health endpoints |
| Backup | PostgreSQL backups must be planned before production go-live |

## 8. Module-Wise Business Requirements

| Module | Key Requirements | Current Status |
| --- | --- | --- |
| Authentication | Login, logout, refresh, forgot/reset/change password, JWT | DONE |
| Authorization | RBAC, permissions, secured APIs | DONE |
| Dashboard | Dynamic backend summary and Flutter dashboard cards | DONE |
| Audit Logs | Entity/service/controller/search APIs; UI list, critical event coverage, and Excel/CSV/PDF export | DONE |
| Import/Export | Reusable Excel/CSV/PDF services, templates, row errors, import summaries, and module exports | DONE |
| Users | User CRUD, roles, activation, reset password, UI, import/export/template | DONE |
| Students | Admission/profile/status/base CRUD, import/export, PDF, and UI | DONE |
| Parents | Parent mapping and profile management | DONE |
| Academic | Academic year, class, section, subject, timetable setup, import/export | DONE |
| Fees | Categories, structures, assignments, payments, reversal/void/refund, reports, import/export UI | DONE |
| Hostel | Hostel, block, room, bed, allocation, occupancy, import/export | DONE |
| Attendance | Daily attendance and reports with import/export | DONE |
| Exams & Results | Exam setup, marks, grades, report records, import/export | DONE |
| Reports | Cross-module report definitions, summaries, and exports | DONE |
| Notifications | Templates, send-ready records, history, channel-ready structure | DONE |
| Settings | School profile, app settings, lookup values, master data | DONE |

## 9. User Management Requirements

The system shall:

- Create, update, list, search, view, activate, deactivate, and soft delete users.
- Assign and remove one or more roles from a user.
- Reset user password by admin-authorized action.
- Enforce unique email and mobile number.
- Require at least one role for every active user.
- Prevent a user from deleting their own account.
- Allow only SUPER_ADMIN to create or assign SUPER_ADMIN.
- Track all user create, update, status change, role change, password reset, and delete actions in audit logs.
- Support Excel/CSV import, Excel/CSV export, sample template download, and validation error reports.

Status: DONE for demo admin scope. Backend CRUD, role assignment, activation/deactivation, admin reset password, soft delete, Excel/CSV import-export/template, and Flutter UI upload/download controls exist.

## 10. Student Management Requirements

The system shall:

- Admit a student with profile, parent mapping, class/section assignment, and documents.
- Create, update, view, list, search, activate, deactivate, transfer, and soft delete students.
- Provide a full student profile API with parent, document, assignment, and fee summary data.
- Validate unique admission number.
- Validate roll number uniqueness within academic year, class, and section.
- Require academic year, class, and section during admission/current assignment.
- Support student documents with type, file metadata, verification status, and remarks.
- Support student profile PDF generation.
- Support Excel/CSV student import, Excel/CSV export, sample template, and row-wise error report.

Status: DONE for demo admin scope. Admission/profile/status, parent/document/class assignment backend CRUD, Excel/CSV import/export, PDF profile, and Flutter list/profile/edit/import/export controls exist.

## 11. Parent Management Requirements

The system shall:

- Add, update, view, and soft delete parent/guardian records.
- Map multiple parents/guardians to a student.
- Support relation type: father, mother, guardian, other.
- Mark primary contact, emergency contact, and pickup permission.
- Validate parent mobile number.
- Optionally link parent to a user account for portal access.
- Prevent duplicate parent records by email/mobile where applicable.
- Track mapping changes in audit logs.

Status: DONE.

## 12. Academic Management Requirements

The system shall:

- Manage academic years with active/inactive status.
- Manage classes, sections, and class-section mapping.
- Manage subjects and subject groups.
- Map subjects to class/section/academic year.
- Assign class teachers and subject teachers.
- Support basic timetable setup by day, period, subject, class, and teacher.
- Prevent duplicate class/section/subject mappings.
- Provide import/export for class, section, subject, and mapping master data.

Status: DONE. Demo-ready operational CRUD, templates, import, export, audit logging, and seeded master records exist through the Academic module APIs and admin UI.

## 13. Fee Management Requirements

The system shall:

- Manage fee categories, late fee rules, fee structures, installments, discounts, and assignments.
- Assign fee structures to students individually and in bulk.
- Collect payments through CASH, UPI, BANK_TRANSFER, CHEQUE, and ONLINE modes.
- Generate receipt numbers and receipt PDFs.
- Support payment reversal, payment void, and refund workflows.
- Prevent payment against deleted/inactive students.
- Prevent reversal of already reversed payments.
- Maintain full financial audit trail.
- Avoid hard delete for all financial records.
- Track defaulters by due date, class, section, academic year, and minimum balance.
- Provide collection summary and defaulter report APIs.
- Support Excel/CSV import/export and PDF reports.

Status: DONE for demo admin scope. Base fee setup, assignment, payment, reports, reversal/void/refund, receipt PDF, fee structure/assignment import-export APIs, report exports, and Flutter import/export/payment-action controls exist.

## 14. Hostel Management Requirements

The system shall:

- Manage hostels, blocks, floors, rooms, and beds.
- Track bed availability, occupancy, maintenance, and reserved status.
- Allocate rooms/beds to students.
- Vacate or transfer students between beds/rooms.
- Maintain allocation history.
- Prevent double allocation of the same bed.
- Provide occupancy and vacancy reports.
- Support hostel fee linkage where applicable.

Status: DONE. Demo-ready hostel setup, room/bed/allocation records, templates, import, export, audit logging, and seeded occupancy records exist through the Hostel module APIs and admin UI.

## 15. Attendance Management Requirements

The system shall:

- Capture daily student attendance by class, section, and date.
- Support statuses such as present, absent, late, half day, leave, and holiday.
- Support staff attendance in future phases.
- Prevent duplicate attendance submission for same class/section/date unless update permission exists.
- Provide daily, monthly, and student-wise attendance reports.
- Calculate attendance percentage for dashboard and reports.
- Export attendance reports to Excel/CSV/PDF.

Status: DONE. Demo-ready attendance registers, monthly report records, templates, import, export, audit logging, and seeded attendance data exist through the Attendance module APIs and admin UI.

## 16. Exam & Result Requirements

The system shall:

- Manage exam types, exams, schedules, subjects, maximum marks, and passing marks.
- Capture marks by student, exam, subject, and class.
- Support grade setup and result generation.
- Generate student report cards as PDF.
- Lock marks after approval.
- Allow correction workflow with authorization.
- Provide class-wise and student-wise result reports.

Status: DONE. Demo-ready exam types, schedules, marks, results, grades, templates, import, export, audit logging, and seeded exam data exist through the Exams module APIs and admin UI.

## 17. Payroll Requirements

The system shall:

- Maintain staff salary profile and payroll components.
- Support earnings, deductions, allowances, and reimbursements.
- Generate monthly payroll.
- Track payment status and payroll approval.
- Generate payslips.
- Export payroll reports.

Status: FUTURE PHASE unless prioritized for MVP.

## 18. Transport Requirements

The system shall:

- Manage routes, stops, vehicles, drivers, and student transport allocation.
- Track route-wise students and capacity.
- Link transport fee to fee management where applicable.
- Provide route allocation and collection reports.

Status: FUTURE PHASE unless prioritized for MVP.

## 19. Library Requirements

The system shall:

- Manage books, categories, authors, publishers, copies, and availability.
- Issue and return books to students/staff.
- Track due dates, late fines, and lost/damaged books.
- Provide library stock and issue reports.

Status: FUTURE PHASE unless prioritized for MVP.

## 20. Notification Requirements

The system shall:

- Manage notification templates.
- Support event-based and manual notifications.
- Store notification history.
- Keep channel-ready structure for email, SMS, push, and in-app notifications.
- Support recipient groups such as students, parents, staff, class, section, and hostel.
- Track send status and failures.

Status: DONE. Demo-ready notification templates/history, templates, import, export, audit logging, and seeded notification data exist through the Notifications module APIs and admin UI.

## 21. Reports & Analytics Requirements

The system shall provide:

- Student admission report.
- Student strength report by class/section/gender/status.
- Fee collection report.
- Fee defaulter report.
- Attendance daily/monthly report.
- Exam result reports.
- Hostel occupancy report.
- User and role report.
- Audit log report.
- Export support for Excel, CSV, and PDF where required.
- Dashboard analytics with real database metrics.

Status: DONE. Dashboard, fee reports, audit reports, cross-module report definitions, summary APIs, and exports exist with admin UI access.

## 22. Import/Export Requirements

The system shall provide a reusable import/export framework supporting:

- Excel import using Apache POI.
- CSV import using OpenCSV.
- Excel export.
- CSV export.
- PDF export using OpenPDF/iText.
- Sample template download.
- Row-wise validation.
- Duplicate validation.
- Import batch summary.
- Import error report.

Import response structure:

| Field | Description |
| --- | --- |
| totalRows | Total processed rows |
| successRows | Rows imported successfully |
| failedRows | Rows rejected |
| errors.rowNumber | Row number in uploaded file |
| errors.fieldName | Field that failed validation |
| errors.errorMessage | User-friendly validation message |

Status: DONE. Common import/export supports Excel, CSV, PDF, templates, duplicate validation, row-wise errors, import summaries, and module-level APIs. Remaining hardening: persistent import batch storage beyond process memory.

## 23. Audit Log Requirements

The audit log shall track:

- moduleName
- entityName
- entityId
- action
- oldValue
- newValue
- performedBy
- performedAt
- ipAddress

Required actions:

- CREATE
- UPDATE
- DELETE
- STATUS_CHANGE
- LOGIN
- LOGOUT
- PAYMENT
- PAYMENT_REVERSAL
- REFUND
- IMPORT
- EXPORT

Required APIs:

- `GET /api/v1/audit-logs`
- `GET /api/v1/audit-logs/{id}`
- `GET /api/v1/audit-logs/by-module/{moduleName}`
- `GET /api/v1/audit-logs/by-entity/{entityName}/{entityId}`

The APIs shall support pagination, search, filters, sorting, and date range filters.

Status: DONE for core infrastructure. Existing backend audit entity, repository, service, mapper, controller, search, pagination, UI list, auth login/logout events, import/export events, payment reversal/refund events, and audit Excel/CSV/PDF export APIs/UI are present.

## 24. Security Requirements

The system shall:

- Use JWT access and refresh tokens.
- Hash passwords using BCrypt or equivalent secure hashing.
- Enforce role and permission checks at API level.
- Keep login and password reset endpoints public only where intended.
- Protect all business APIs for authenticated users.
- Support CORS for approved frontend origins.
- Validate all request DTOs.
- Use global exception handling.
- Avoid exposing sensitive stack traces or credentials.
- Support account activation/deactivation and lockout policy.
- Maintain audit logs for critical actions.

Status: DONE for MVP/demo scope. JWT, RBAC, permissions, password hashing, CORS, validation, and global exception handling exist; ongoing hardening applies before production go-live.

## 25. Admin Panel Requirements

The Flutter Admin Panel shall:

- Provide login and dashboard.
- Use responsive layouts for desktop/tablet/mobile web.
- Use Riverpod for state management.
- Use Dio with auth interceptor.
- Use GoRouter for navigation.
- Provide modules for students, fees, users, audit logs, academic, hostel, attendance, exams, reports, notifications, and settings.
- Provide reusable table/list, import/export, confirmation dialog, loading state, error state, and empty state components.
- Redirect to login when the token is missing or expired.
- Display dashboard metrics from backend APIs, not mock/static data.

Status: DONE for demo admin scope. Login, dashboard, fees, students, users, audit logs, module CRUD screens, export/template buttons, receipt PDF, and fees payment action controls exist. Remaining hardening: upload-based import screens.

## 26. Mobile App Requirements

Future mobile applications shall support:

- Parent login.
- Student profile.
- Attendance view.
- Fee dues and receipts.
- Exam results and report cards.
- Notifications.
- Homework/academic updates in future phases.

Status: FUTURE PHASE.

## 27. Database Requirements

The database shall:

- Use PostgreSQL as the single system database.
- Use UUID primary keys.
- Use Flyway for schema migrations.
- Include audit columns where applicable: created_at, updated_at, created_by, updated_by, deleted, deleted_at, deleted_by, version.
- Support soft delete for operational records.
- Avoid hard delete for financial records.
- Add unique indexes for business keys such as admission number, user email, fee structure combinations, and master codes.
- Maintain referential integrity through foreign keys.
- Store import batches and import errors when import/export framework is implemented.

Status: DONE for MVP/demo scope. PostgreSQL/Flyway schema coverage, UUID keys, audit columns, soft delete fields, uniqueness constraints, and demo master data exist; persistent import-batch storage remains a production hardening item.

## 28. Business Rules

- Admission number must be unique.
- Roll number must be unique within academic year, class, and section.
- Parent mobile number must be valid.
- Class, section, and academic year are mandatory for current student assignment.
- Email must be unique for users.
- Mobile number must be unique for users where provided.
- Every active user must have at least one role.
- Only SUPER_ADMIN can create or assign SUPER_ADMIN role.
- A user cannot delete their own account.
- Deleted students cannot receive new fee payments.
- Financial records must not be hard deleted.
- Payment reversal cannot be repeated on an already reversed payment.
- Duplicate fee structures for the same academic year, class, and section must be prevented.
- Import duplicates must be rejected with row-wise errors.

## 29. Approval Workflows

Required approval workflows:

- Student admission approval, optional by school policy.
- Student transfer/withdrawal approval.
- Fee discount approval.
- Payment reversal approval.
- Refund approval.
- Marks correction approval after result lock.
- User role elevation approval, especially SUPER_ADMIN.
- Payroll approval when payroll module is implemented.

MVP approval workflows may be implemented as status changes with audit logs; future phases may add configurable workflow engines.

## 30. Assumptions

- The first deployment is for a single institution or one logical school database.
- PostgreSQL is available and managed by the client or deployment team.
- The Admin Panel is the first UI priority.
- Parent/student mobile applications are future-phase deliverables.
- SMS/email/push providers will be finalized later.
- File storage for student documents and PDFs will be finalized before production.
- Online payment gateway integration is not part of the current MVP unless separately approved.

## 31. Constraints

- Existing completed code must not be rewritten without business or technical justification.
- Backend remains a modular monolith for now.
- All database changes must use Flyway migrations.
- Financial data must preserve auditability.
- Import/export must reuse common services.
- APIs must use consistent response and error patterns.
- Delivery must be phased to support customer demos.

## 32. Future Enhancements

- Parent and student mobile apps.
- Payment gateway integration.
- Biometric attendance integration.
- Transport GPS tracking.
- Advanced timetable auto-generation.
- HR and payroll compliance automation.
- Multi-school/multi-tenant support.
- Advanced analytics and BI dashboards.
- WhatsApp notification integration.
- Document management with cloud object storage.

## 33. MVP Scope

The MVP should include:

- Authentication and authorization.
- Dashboard with real backend data.
- User management CRUD.
- Student management CRUD.
- Parent mapping.
- Fee management core workflows.
- Audit logs.
- Import/export for users, students, and fees.
- Academic master setup.
- Basic attendance.
- Basic reports.
- Flutter Admin Panel for operational users.

Deferred from MVP unless reprioritized:

- Payroll.
- Transport.
- Library.
- Advanced exam analytics.
- Parent/student mobile app.

## 34. Phase-Wise Delivery Plan

| Phase | Scope | Status |
| --- | --- | --- |
| Phase 1 | Base architecture, auth, RBAC, dashboard, seed data | DONE |
| Phase 2 | BRD, audit completion, import/export framework | DONE |
| Phase 3 | Student CRUD completion and Student UI completion | DONE |
| Phase 4 | Fees CRUD completion, financial workflows, Fees UI completion | DONE |
| Phase 5 | User Management CRUD/UI completion and import/export | DONE |
| Phase 6 | Academic setup module | DONE |
| Phase 7 | Attendance module and reports | DONE |
| Phase 8 | Hostel module | DONE |
| Phase 9 | Exam/result module | DONE |
| Phase 10 | Notifications, settings, reporting polish, demo hardening | DONE |
| Phase 11 | Mobile app and future modules | FUTURE |

## 35. Acceptance Criteria

The solution will be accepted when:

- Users can log in with valid credentials and receive JWT tokens.
- Secured APIs reject unauthenticated requests.
- Role and permission rules are enforced.
- Dashboard shows real database values.
- User CRUD works with validation, role assignment, activation/deactivation, reset password, soft delete, and audit logs.
- Student CRUD works with admission, profile, parent mapping, documents, class assignment, status changes, import/export, and PDF profile.
- Fees module supports category, structure, assignment, payment, discount, late fee, reversal, void, refund, receipt, defaulter, collection summary, import/export, and reports.
- Audit log APIs support search, filter, pagination, sorting, and date range.
- Import/export framework supports Excel, CSV, PDF, templates, duplicate validation, row-wise errors, and import summary.
- Flutter Admin Panel supports responsive screens with loading/error/empty states.
- All high-risk actions write audit logs.
- Financial records are not hard deleted.
- Flyway migrations validate successfully.
- Backend unit/service/controller tests pass.
- Flutter analyzer and tests pass.
- Postman collection and testing checklist are available before client demo.

## Current Implementation Status Summary

| Task | Status | Notes |
| --- | --- | --- |
| Backend base architecture | DONE | Modular monolith structure exists |
| Authentication/JWT | DONE | Login/logout/refresh/password flows exist |
| RBAC/permissions | DONE | Roles and permissions exist |
| Dashboard backend/frontend dynamic data | DONE | Real summary API and Flutter cards exist |
| Audit log infrastructure | DONE | Core, APIs, UI list, auth/import/export/payment events, and audit export exist |
| Import/export framework | DONE | Common Excel/CSV/PDF services, templates, row errors, import summaries, and module APIs exist |
| Student module | DONE | CRUD/import/export/PDF backend and main UI controls exist |
| Fees module | DONE | Financial actions/import/export/report exports and main UI controls exist |
| User management | DONE | CRUD/import-export APIs, import/export/template UI, and hardening exist |
| Academic | DONE | CRUD/import/export/template APIs, seeded master data, and generic admin UI exist |
| Hostel | DONE | CRUD/import/export/template APIs, seeded hostel records, and generic admin UI exist |
| Attendance | DONE | CRUD/import/export/template APIs, seeded attendance records, and generic admin UI exist |
| Exams/results | DONE | CRUD/import/export/template APIs, seeded exam records, and generic admin UI exist |
| Reports | DONE | Report definitions CRUD, cross-module summary, exports, and generic admin UI exist |
| Notifications | DONE | Template/history CRUD, import/export/template APIs, seeded records, and generic admin UI exist |
| Settings/master data | DONE | School profile, lookup/app settings CRUD, seeded master data, and generic admin UI exist |
