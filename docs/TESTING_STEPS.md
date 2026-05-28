# School ERP Testing Steps

## 1. Prerequisites
- Java 21, PostgreSQL, Flutter SDK, Chrome/Edge for Flutter web.
- Use `JAVA_HOME=C:\Program Files\Java\jdk-21.0.4` on this machine.

## 2. Database setup
- Start PostgreSQL and create the configured database/user from `src/main/resources/application.yml`.
- For local QA, use the existing Docker/PostgreSQL setup in `docker-compose.yml` if preferred.

## 3. Flyway migration steps
- Run the backend once; Flyway applies `V1` through the latest migration automatically.
- New migrations add student photo metadata and class mappings for fee structures/assignments.

## 4. Backend run command
```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.4'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
./mvnw spring-boot:run
```

## 5. Flutter run command
```powershell
cd frontend
flutter pub get
flutter run -d chrome
```

## 6. Login credentials
- Check `scripts/login_credentials.txt`.
- Default demo login in the Postman collection is `admin@school.com` / `Admin@12345678`.

## 7. Master data verification
- Verify academic year `2026-2027`, classes, sections, subjects, and demo teachers.
- Verify fee categories and permissions are present.

## 8. Student add/edit test cases
- Add a student from a class/section screen.
- Enter first, middle, last name, DOB, gender, blood group, mobile, email, address, city/state/pin, admission date, status, guardian, and roll number.
- Open profile and confirm middle name and all entered fields are saved.
- Edit personal details and confirm audit log entry.

## 9. Student profile tab test cases
- Personal Details: edit details, update photo URL/storage key, activate/deactivate.
- Parent / Guardian: add, edit, remove, mark primary.
- Academic Details: assign/change class/section and roll number.
- Documents: upload metadata, edit, delete.
- Attendance, Fees, Hostel, Transport: verify empty states/actions render cleanly.

## 10. Parent/guardian test cases
- Add father, mother, and guardian.
- Attempt duplicate relation mapping and confirm validation error.
- Mark a new primary contact and verify only one primary remains.

## 11. Academic class mapping test cases
- Open Student Management by academic year.
- Confirm classes/sections come from Academic hierarchy tables.
- Assign a student to a class/section and verify the class section roster.

## 12. Fee structure test cases
- Create a class fee structure for an academic year and class.
- Add categories and installments with matching totals.
- Verify duplicate class/year fee structure is rejected.

## 13. Class fee assignment test cases
- Call `POST /api/v1/fees/classes/{classId}/assign`.
- Verify assignments are generated for students in that class.
- Re-run and confirm existing assignments are skipped.

## 14. Payment collection test cases
- Collect payment from assignment endpoint or student payment endpoint.
- Verify paid and balance amounts update.

## 15. Payment history test cases
- Call `GET /api/v1/fees/students/{studentId}/payment-history`.
- Verify receipts and payment rows appear after collection.

## 16. Template download test cases
- Download student, user, fee structure, fee assignment, and academic class templates.
- Confirm each file has headers and a sample row.

## 17. Import valid file test cases
- Use files in `src/main/resources/sample-imports`.
- Import valid student and fee structure CSV/XLSX files.
- Confirm success count is at least 5 for valid samples.

## 18. Import invalid file test cases
- Import `students_import_sample_invalid.csv/xlsx`.
- Confirm failures include missing admission number, duplicate admission number, invalid mobile, missing class/section, and invalid date/gender.

## 19. Export test cases
- Export students, fees, audit logs, and reports in supported formats.
- Confirm downloaded files open correctly.

## 20. Audit log filter test cases
- Use module, action, performed-by, and date-range filters.
- Reset filters and export filtered audit logs.

## 21. Role/permission test cases
- Verify users without manage permissions cannot create/update/delete.
- Verify read-only users can view lists and downloads only.

## 22. Expected results
- All successful operations show success messages.
- Failed operations show backend validation messages.
- Financial deletes are soft/cancel flows, not destructive hard deletes.
- Audit logs are written for critical student, parent, class, fee, payment, import, and export actions.

## 23. Common troubleshooting
- If Maven reports record syntax errors, check `java -version`; Java 8 is first on PATH on this machine.
- If Flutter cannot reach APIs, verify backend URL in `frontend/lib/src/core/config/app_config.dart`.
- If imports fail, compare headers with the template endpoints.
- If Flyway fails, inspect the newest migration and database schema state before retrying.
