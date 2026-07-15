# Documents Testing

Date: 2026-07-15

## Implemented Design

- Certificates are generated from existing ERP student and academic data.
- Generated PDFs are stored in `generated_documents` with metadata, content, status, and reprint history.
- Legacy certificate import was intentionally excluded. Existing/scanned certificates should use the existing student document upload path until a real migration requirement is confirmed.

## APIs

- `POST /api/v1/documents/certificates/preview`
- `POST /api/v1/documents/certificates/generate`
- `GET /api/v1/documents/{documentId}/download`
- `GET /api/v1/students/{studentId}/documents/generated`
- `GET /api/v1/documents/{documentId}`
- `POST /api/v1/documents/{documentId}/reprint`

## Test Data

- Student: `{{studentId}}`
- Academic year: `{{academicYearId}}`
- Generated document: `{{documentId}}`
- Document types: `BONAFIDE_CERTIFICATE`, `LEAVING_CERTIFICATE`, `STUDENT_ID_CARD`

## Scenarios

1. Generate Bonafide for active student
   - `POST /api/v1/documents/certificates/generate`
   - Expected: metadata response with document ID and downloadable PDF content stored.

2. Preview certificate
   - `POST /api/v1/documents/certificates/preview`
   - Expected: PDF bytes returned, no history row created.

3. Download PDF
   - `GET /api/v1/documents/{documentId}/download`
   - Expected: `application/pdf` response and audit log.

4. Verify student/class/year data
   - Expected: generated PDF uses current student profile and class assignment data from the ERP.

5. Generate LC for eligible student
   - Expected: one original generated LC is allowed per student.

6. Reject duplicate LC
   - Expected: second original LC returns conflict and instructs reprint.

7. Reprint LC
   - `POST /api/v1/documents/{documentId}/reprint`
   - Expected: separate history row with `REPRINTED` status and `reprintOfDocumentId`.

8. Verify original remains unchanged
   - Expected: original generated row remains `GENERATED`.

9. Verify document history
   - `GET /api/v1/students/{studentId}/documents/generated`
   - Expected: generated and reprinted rows ordered newest first.

10. Generate Student ID
    - Expected: PDF generated and stored.

11. Generate certificates in bulk
    - Status: not implemented in this pass.

12. Upload a scanned legacy certificate
    - Status: use existing student document upload metadata path; no certificate CSV import added.

13. Verify permissions
    - Preview/download/history require `STUDENTS_READ`.
    - Generate/reprint require `STUDENTS_UPDATE`.

14. Verify audit logs
    - Preview, generate, download, and reprint record `DOCUMENTS` audit events.

15. Test direct download after browser reload
    - Expected: download works from stored `documentId` without relying on in-memory state.

## Actual Automated Results

- Backend compile passed with JDK 21 after adding the documents module.
- Dedicated document service tests are still pending.
