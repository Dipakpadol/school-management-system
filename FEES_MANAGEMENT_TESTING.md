# Fees Management Testing

Date: 2026-07-15

## Test Data

- Base URL: `{{baseUrl}}` default `http://localhost:8080/api`
- Auth: Bearer token with `FEES_READ`, `FEES_MANAGE`, `STUDENTS_READ`, and `STUDENTS_UPDATE`
- Academic year: `{{academicYearId}}`
- Class: `{{classId}}`
- Section: `{{sectionId}}`
- Student: `{{studentId}}`
- Fee category: `{{feeCategoryId}}`
- Fee structure: `{{feeStructureId}}`
- Student fee assignment: `{{assignmentId}}`
- Payment: `{{paymentId}}`

## Actual Automated Results

- Reproduced root cause by code-path inspection and regression coverage: class assignment had no persisted class-level record, so later students could not inherit the selected class assignment.
- `./mvnw -q -DskipTests compile`: PASS with JDK 21.
- `./mvnw -q -Dtest=FeeServiceTest test`: PASS with JDK 21.
- Initial test run with Java 8 failed before execution; rerun with `JAVA_HOME=C:\Program Files\Java\jdk-21.0.4`.
- Flutter formatting/analyze could not complete because local `dart`/`flutter` commands hung in this shell.

## API Scenarios

1. Create fee category
   - `POST /api/v1/fees/categories`
   - Expected: category created, unique code enforced, audit log written.
   - Actual: existing CRUD path verified by compile; manual API execution pending.

2. Edit fee category
   - `PUT /api/v1/fees/categories/{id}`
   - Expected: category updated, duplicate code rejected.

3. Create fee structure for academic year + class
   - `POST /api/v1/fees/structures`
   - Expected: active structures are unique by academic year + class + section + structure name.

4. Edit fee structure
   - `PUT /api/v1/fees/structures/{id}`
   - Expected: assigned structures remain protected from silent overwrite.

5. Open Assignments screen
   - Expected: academic year/class dropdowns load, active fee structures load by IDs, assigned badges load from class assignment API.

6. Assign fee structure to class
   - `POST /api/v1/fees/classes/{classId}/assign`
   - Expected: class assignment row created, student assignments created for current students, duplicates skipped.
   - Actual: covered by `FeeServiceTest.assignFeeToClassAcceptsMultipleFeeStructuresAndSkipsExistingAssignments`.

7. Verify current class students receive assignments
   - `GET /api/v1/fees/classes/{classId}/students?academicYearId=...`
   - Expected: totals reflect all assigned structures.

8. Add a new student after class assignment
   - Expected: saved class assignment is used for auto-assignment.
   - Actual: covered by `FeeServiceTest.assignActiveClassFeesToStudentUsesPersistedClassAssignments`.

9. Verify assignments list
   - `GET /api/v1/fees/assignments`
   - Expected: student fee assignments show source `CLASS`.

10. Verify student profile Fees tab
   - Expected: class/hostel/transport groups render from student fee summary.

11. Collect partial payment
   - `POST /api/v1/fees/students/{studentId}/payments`
   - Expected: pending amount decreases and status becomes `PARTIALLY_PAID`.

12. Collect remaining payment
   - Expected: status becomes `PAID`.

13. Verify payment history
   - `GET /api/v1/fees/students/{studentId}/payment-history`

14. Verify receipt generation/download
   - `GET /api/v1/fees/payments/{paymentId}/receipt`

15. Verify duplicate payment reference validation
   - Expected: duplicate non-cash reference is rejected.
   - Actual: existing `FeeServiceTest` covers duplicate reference validation.

16. Verify defaulter list
   - `GET /api/v1/fees/defaulters`
   - Expected: unpaid overdue assignments only.

17. Verify CLASS/HOSTEL/TRANSPORT grouping
   - `GET /api/v1/fees/students/{studentId}/summary?academicYearId=...`

18. Verify discount and late fee
   - Existing service tests cover discount and late fee assessment.

19. Verify void/reversal/refund
   - Existing endpoints are present; full E2E pending.

20. Verify exports
   - Assignments and defaulters export endpoints are present; manual file validation pending.

21. Verify audit logs
   - New class assignment, student auto-assignment, payment, receipt, and export paths record audit events.

## Remaining Manual QA

- Browser E2E for assignment screen badges and provider refresh.
- Full payment collection with multiple selected assignment IDs is still limited by the existing single-assignment receipt model.
- Export file contents need manual validation after running the API against PostgreSQL.
