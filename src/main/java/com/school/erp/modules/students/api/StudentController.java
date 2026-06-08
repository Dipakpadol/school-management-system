package com.school.erp.modules.students.api;

import java.util.UUID;
import java.util.List;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.exams.api.dto.StudentExamResultsResponse;
import com.school.erp.modules.exams.application.ExamService;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentRequest;
import com.school.erp.modules.students.api.dto.ParentMappingRequest;
import com.school.erp.modules.students.api.dto.ParentMappingResponse;
import com.school.erp.modules.students.api.dto.StudentAdmissionRequest;
import com.school.erp.modules.students.api.dto.StudentDocumentRequest;
import com.school.erp.modules.students.api.dto.StudentPhotoRequest;
import com.school.erp.modules.students.api.dto.StudentProfileRequest;
import com.school.erp.modules.students.api.dto.StudentResponse;
import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.api.dto.StudentStatusUpdateRequest;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.application.StudentImportExportService;
import com.school.erp.modules.students.application.StudentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Admissions, profiles, parent mappings, documents, and class assignments.")
public class StudentController {

	private final StudentService studentService;
	private final StudentImportExportService studentImportExportService;
	private final AttendanceService attendanceService;
	private final ExamService examService;

	@PostMapping("/admissions")
	@PreAuthorize("hasAuthority('STUDENTS_CREATE')")
	@Operation(summary = "Admit a student", description = "Creates a student profile with parent mappings, initial class assignment, and document metadata.")
	@io.swagger.v3.oas.annotations.responses.ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Student admitted"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Admission number already exists")
	})
	public ResponseEntity<ApiResponse<StudentResponse>> admitStudent(
			@Valid @RequestBody StudentAdmissionRequest request,
			HttpServletRequest httpRequest) {
		StudentResponse response = studentService.admitStudent(request);
		return created(response, "Student admitted successfully", httpRequest);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Search students", description = "Searches and filters students with pagination.")
	public ResponseEntity<ApiResponse<PageResponse<StudentSummaryResponse>>> searchStudents(
			@Valid @ParameterObject StudentSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		PageResponse<StudentSummaryResponse> response = studentService.search(searchRequest, pageRequest);
		return ok(response, "Students fetched successfully", httpRequest);
	}

	@PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('STUDENTS_CREATE')")
	@Operation(summary = "Import students from Excel")
	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(studentImportExportService.importExcel(file), "Student Excel import completed", httpRequest);
	}

	@PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('STUDENTS_CREATE')")
	@Operation(summary = "Import students from CSV")
	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			@RequestParam("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		return ok(studentImportExportService.importCsv(file), "Student CSV import completed", httpRequest);
	}

	@GetMapping("/export/excel")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Export students to Excel")
	public ResponseEntity<byte[]> exportExcel() {
		return file(
				studentImportExportService.exportExcel(),
				"students.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/export/csv")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Export students to CSV")
	public ResponseEntity<byte[]> exportCsv() {
		return file(studentImportExportService.exportCsv(), "students.csv", "text/csv");
	}

	@GetMapping("/template")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Download student Excel import template")
	public ResponseEntity<byte[]> template() {
		return file(
				studentImportExportService.excelTemplate(),
				"student-import-template.xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	@GetMapping("/template/csv")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Download student CSV import template")
	public ResponseEntity<byte[]> csvTemplate() {
		return file(studentImportExportService.csvTemplate(), "student-import-template.csv", "text/csv");
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get student import validation errors")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest httpRequest) {
		return ok(studentImportExportService.importErrors(batchId), "Student import errors fetched successfully", httpRequest);
	}

	@GetMapping("/{studentId}")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get student profile", description = "Fetches full student profile, parent mappings, documents, and class assignments.")
	public ResponseEntity<ApiResponse<StudentResponse>> getStudentProfile(
			@Parameter(description = "Student UUID") @PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.getStudentProfile(studentId), "Student profile fetched successfully", httpRequest);
	}

	@GetMapping("/{studentId}/profile")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get student full profile", description = "Fetches full student profile for profile tabs.")
	public ResponseEntity<ApiResponse<StudentResponse>> getStudentFullProfile(
			@Parameter(description = "Student UUID") @PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.getStudentProfile(studentId), "Student profile fetched successfully", httpRequest);
	}

	@GetMapping("/{studentId}/attendance-history")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ','ATTENDANCE_READ')")
	@Operation(summary = "Get student attendance history", description = "Reads attendance management records for a student profile.")
	public ResponseEntity<ApiResponse<StudentAttendanceHistoryResponse>> getStudentAttendanceHistory(
			@PathVariable UUID studentId,
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) java.time.LocalDate fromDate,
			@RequestParam(required = false) java.time.LocalDate toDate,
			@RequestParam(required = false) AttendanceStatus status,
			@RequestParam(required = false) String sort,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(
				attendanceService.getStudentAttendanceHistory(studentId, academicYearId, fromDate, toDate, status, pageRequest, sort),
				"Student attendance history fetched successfully",
				httpRequest);
	}

	@GetMapping("/{studentId}/attendance-history/export")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ','ATTENDANCE_READ')")
	@Operation(summary = "Export student attendance history")
	public ResponseEntity<byte[]> exportStudentAttendanceHistory(
			@PathVariable UUID studentId,
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) java.time.LocalDate fromDate,
			@RequestParam(required = false) java.time.LocalDate toDate,
			@RequestParam(required = false) AttendanceStatus status) {
		return file(
				attendanceService.exportStudentAttendanceHistory(studentId, academicYearId, fromDate, toDate, status),
				"student-attendance-" + studentId + ".csv",
				"text/csv");
	}

	@GetMapping("/{studentId}/exam-results")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ','EXAMS_READ')")
	@Operation(summary = "Get student exam results", description = "Reads exam marks and schedules for a student profile.")
	public ResponseEntity<ApiResponse<StudentExamResultsResponse>> getStudentExamResults(
			@PathVariable UUID studentId,
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID examTypeId,
			@RequestParam(required = false) UUID examScheduleId,
			HttpServletRequest httpRequest) {
		return ok(
				examService.getStudentProfileExamResults(studentId, academicYearId, examTypeId, examScheduleId),
				"Student exam results fetched successfully",
				httpRequest);
	}

	@GetMapping("/{studentId}/exam-results/{resultId}/report-card")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ','EXAMS_READ')")
	@Operation(summary = "Export student profile report card")
	public ResponseEntity<byte[]> exportStudentProfileReportCard(
			@PathVariable UUID studentId,
			@PathVariable UUID resultId,
			@RequestParam(required = false) UUID academicYearId) {
		return file(
				examService.exportStudentProfileReportCard(studentId, resultId, academicYearId),
				"student-report-card-" + studentId + ".csv",
				"text/csv");
	}

	@GetMapping("/{studentId}/parents")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Get parents by student", description = "Fetches parent and guardian mappings for a student.")
	public ResponseEntity<ApiResponse<List<ParentMappingResponse>>> getParents(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.getParents(studentId), "Student parents fetched successfully", httpRequest);
	}

	@GetMapping("/{studentId}/pdf")
	@PreAuthorize("hasAuthority('STUDENTS_READ')")
	@Operation(summary = "Export student profile PDF")
	public ResponseEntity<byte[]> profilePdf(@PathVariable UUID studentId) {
		return file(studentImportExportService.profilePdf(studentId), "student-profile-" + studentId + ".pdf", "application/pdf");
	}

	@PutMapping("/{studentId}/profile")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Update student profile", description = "Updates demographic, contact, admission, and address details.")
	public ResponseEntity<ApiResponse<StudentResponse>> updateStudentProfile(
			@PathVariable UUID studentId,
			@Valid @RequestBody StudentProfileRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updateStudentProfile(studentId, request), "Student profile updated successfully", httpRequest);
	}

	@PutMapping("/{studentId}/photo")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Upload or update student photo metadata")
	public ResponseEntity<ApiResponse<StudentResponse>> updatePhoto(
			@PathVariable UUID studentId,
			@Valid @RequestBody StudentPhotoRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updatePhoto(studentId, request), "Student photo updated successfully", httpRequest);
	}

	@PostMapping("/{studentId}/parents")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Add parent mapping", description = "Maps a parent or guardian to the student.")
	public ResponseEntity<ApiResponse<StudentResponse>> addParent(
			@PathVariable UUID studentId,
			@Valid @RequestBody ParentMappingRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.addParent(studentId, request), "Parent mapped successfully", httpRequest);
	}

	@PutMapping("/{studentId}/parents/{parentMappingId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Update parent mapping", description = "Updates mapped parent or guardian details and relationship flags.")
	public ResponseEntity<ApiResponse<StudentResponse>> updateParent(
			@PathVariable UUID studentId,
			@PathVariable UUID parentMappingId,
			@Valid @RequestBody ParentMappingRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updateParent(studentId, parentMappingId, request), "Parent mapping updated successfully", httpRequest);
	}

	@DeleteMapping("/{studentId}/parents/{parentMappingId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Delete parent mapping", description = "Soft deletes a parent or guardian mapping.")
	public ResponseEntity<ApiResponse<StudentResponse>> deleteParent(
			@PathVariable UUID studentId,
			@PathVariable UUID parentMappingId,
			HttpServletRequest httpRequest) {
		return ok(studentService.deleteParent(studentId, parentMappingId), "Parent mapping deleted successfully", httpRequest);
	}

	@PostMapping("/{studentId}/documents")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Add student document", description = "Adds document metadata to a student profile.")
	public ResponseEntity<ApiResponse<StudentResponse>> addDocument(
			@PathVariable UUID studentId,
			@Valid @RequestBody StudentDocumentRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.addDocument(studentId, request), "Student document added successfully", httpRequest);
	}

	@PutMapping("/{studentId}/documents/{documentId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Update student document", description = "Updates document metadata.")
	public ResponseEntity<ApiResponse<StudentResponse>> updateDocument(
			@PathVariable UUID studentId,
			@PathVariable UUID documentId,
			@Valid @RequestBody StudentDocumentRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updateDocument(studentId, documentId, request), "Student document updated successfully", httpRequest);
	}

	@DeleteMapping("/{studentId}/documents/{documentId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Delete student document", description = "Soft deletes document metadata.")
	public ResponseEntity<ApiResponse<StudentResponse>> deleteDocument(
			@PathVariable UUID studentId,
			@PathVariable UUID documentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.deleteDocument(studentId, documentId), "Student document deleted successfully", httpRequest);
	}

	@PostMapping("/{studentId}/class-assignments")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Assign class and section", description = "Creates a current class-section assignment and closes the previous active assignment.")
	public ResponseEntity<ApiResponse<StudentResponse>> assignClassSection(
			@PathVariable UUID studentId,
			@Valid @RequestBody ClassSectionAssignmentRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.assignClassSection(studentId, request), "Class and section assigned successfully", httpRequest);
	}

	@PutMapping("/{studentId}/class-assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Update class assignment", description = "Updates the current class and section assignment.")
	public ResponseEntity<ApiResponse<StudentResponse>> updateClassAssignment(
			@PathVariable UUID studentId,
			@PathVariable UUID assignmentId,
			@Valid @RequestBody ClassSectionAssignmentRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updateClassAssignment(studentId, assignmentId, request), "Class assignment updated successfully", httpRequest);
	}

	@DeleteMapping("/{studentId}/class-assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Delete class assignment", description = "Soft deletes class assignment metadata.")
	public ResponseEntity<ApiResponse<StudentResponse>> deleteClassAssignment(
			@PathVariable UUID studentId,
			@PathVariable UUID assignmentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.deleteClassAssignment(studentId, assignmentId), "Class assignment deleted successfully", httpRequest);
	}

	@PatchMapping("/{studentId}/status")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Update student status", description = "Updates student status to ACTIVE, INACTIVE, or TRANSFERRED.")
	public ResponseEntity<ApiResponse<StudentResponse>> updateStatus(
			@PathVariable UUID studentId,
			@Valid @RequestBody StudentStatusUpdateRequest request,
			HttpServletRequest httpRequest) {
		return ok(studentService.updateStatus(studentId, request), "Student status updated successfully", httpRequest);
	}

	@PatchMapping("/{studentId}/activate")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Activate student", description = "Marks a student as ACTIVE.")
	public ResponseEntity<ApiResponse<StudentResponse>> activate(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.activate(studentId), "Student activated successfully", httpRequest);
	}

	@PatchMapping("/{studentId}/deactivate")
	@PreAuthorize("hasAuthority('STUDENTS_UPDATE')")
	@Operation(summary = "Deactivate student", description = "Marks a student as INACTIVE.")
	public ResponseEntity<ApiResponse<StudentResponse>> deactivate(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		return ok(studentService.deactivate(studentId), "Student deactivated successfully", httpRequest);
	}

	@DeleteMapping("/{studentId}")
	@PreAuthorize("hasAuthority('STUDENTS_DELETE')")
	@Operation(summary = "Soft delete student", description = "Soft deletes a student profile.")
	public ResponseEntity<ApiResponse<Void>> deleteStudent(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		studentService.deleteStudent(studentId);
		return ok(null, "Student deleted successfully", httpRequest);
	}

	@DeleteMapping("/{studentId}/permanent")
	@PreAuthorize("hasRole('SUPER_ADMIN')")
	@Operation(summary = "Permanently delete student", description = "Physically deletes a student. Intended for demo/admin cleanup only.")
	public ResponseEntity<ApiResponse<Void>> permanentlyDeleteStudent(
			@PathVariable UUID studentId,
			HttpServletRequest httpRequest) {
		studentService.permanentlyDeleteStudent(studentId);
		return ok(null, "Student permanently deleted successfully", httpRequest);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(response(data, message, request));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(response(data, message, request));
	}

	private <T> ApiResponse<T> response(T data, String message, HttpServletRequest request) {
		return ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID));
	}

	private ResponseEntity<byte[]> file(byte[] content, String filename, String contentType) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(MediaType.parseMediaType(contentType))
				.body(content);
	}
}
