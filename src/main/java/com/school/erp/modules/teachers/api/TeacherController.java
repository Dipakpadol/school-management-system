package com.school.erp.modules.teachers.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentRequest;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherDocumentRequest;
import com.school.erp.modules.teachers.api.dto.TeacherDocumentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherProfileResponse;
import com.school.erp.modules.teachers.api.dto.TeacherRequest;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;
import com.school.erp.modules.teachers.application.TeacherService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "Teacher Management", description = "Teacher CRUD, academic-year mapping, profiles, assignments, and documents.")
public class TeacherController {

	private final TeacherService teacherService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAuthority('TEACHERS_READ')")
	@Operation(summary = "List academic years for teacher management")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(teacherService.academicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping
	@PreAuthorize("hasAnyAuthority('TEACHERS_READ','ACADEMIC_READ')")
	@Operation(summary = "List teachers")
	public ResponseEntity<ApiResponse<List<TeacherResponse>>> teachers(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(teacherService.teachers(academicYearId), "Teachers fetched successfully", request);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Create teacher")
	public ResponseEntity<ApiResponse<TeacherResponse>> createTeacher(
			@Valid @RequestBody TeacherRequest body,
			HttpServletRequest request) {
		return created(teacherService.createTeacher(body), "Teacher created successfully", request);
	}

	@PutMapping("/{teacherId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Update teacher")
	public ResponseEntity<ApiResponse<TeacherResponse>> updateTeacher(
			@PathVariable UUID teacherId,
			@Valid @RequestBody TeacherRequest body,
			HttpServletRequest request) {
		return ok(teacherService.updateTeacher(teacherId, body), "Teacher updated successfully", request);
	}

	@GetMapping("/{teacherId}")
	@PreAuthorize("hasAnyAuthority('TEACHERS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get teacher")
	public ResponseEntity<ApiResponse<TeacherResponse>> getTeacher(
			@PathVariable UUID teacherId,
			HttpServletRequest request) {
		return ok(teacherService.getTeacher(teacherId), "Teacher fetched successfully", request);
	}

	@DeleteMapping("/{teacherId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Soft delete teacher")
	public ResponseEntity<ApiResponse<TeacherResponse>> deleteTeacher(
			@PathVariable UUID teacherId,
			HttpServletRequest request) {
		return ok(teacherService.deleteTeacher(teacherId), "Teacher deleted successfully", request);
	}

	@GetMapping("/{teacherId}/profile")
	@PreAuthorize("hasAuthority('TEACHERS_READ')")
	@Operation(summary = "Get teacher profile")
	public ResponseEntity<ApiResponse<TeacherProfileResponse>> teacherProfile(
			@PathVariable UUID teacherId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(teacherService.profile(teacherId, academicYearId), "Teacher profile fetched successfully", request);
	}

	@GetMapping("/{teacherId}/assignments")
	@PreAuthorize("hasAuthority('TEACHERS_READ')")
	@Operation(summary = "List teacher assignments")
	public ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> assignments(
			@PathVariable UUID teacherId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(teacherService.assignments(teacherId, academicYearId), "Teacher assignments fetched successfully", request);
	}

	@PostMapping("/{teacherId}/assignments")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Create teacher assignment")
	public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> createAssignment(
			@PathVariable UUID teacherId,
			@Valid @RequestBody TeacherAssignmentRequest body,
			HttpServletRequest request) {
		return created(teacherService.createAssignment(teacherId, body), "Teacher assignment created successfully", request);
	}

	@PutMapping("/{teacherId}/assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Update teacher assignment")
	public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> updateAssignment(
			@PathVariable UUID teacherId,
			@PathVariable UUID assignmentId,
			@Valid @RequestBody TeacherAssignmentRequest body,
			HttpServletRequest request) {
		return ok(teacherService.updateAssignment(teacherId, assignmentId, body), "Teacher assignment updated successfully", request);
	}

	@DeleteMapping("/{teacherId}/assignments/{assignmentId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Soft delete teacher assignment")
	public ResponseEntity<ApiResponse<TeacherAssignmentResponse>> deleteAssignment(
			@PathVariable UUID teacherId,
			@PathVariable UUID assignmentId,
			HttpServletRequest request) {
		return ok(teacherService.deleteAssignment(teacherId, assignmentId), "Teacher assignment deleted successfully", request);
	}

	@GetMapping("/{teacherId}/documents")
	@PreAuthorize("hasAuthority('TEACHERS_READ')")
	@Operation(summary = "List teacher documents")
	public ResponseEntity<ApiResponse<List<TeacherDocumentResponse>>> documents(
			@PathVariable UUID teacherId,
			HttpServletRequest request) {
		return ok(teacherService.documents(teacherId), "Teacher documents fetched successfully", request);
	}

	@PostMapping("/{teacherId}/documents")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Create teacher document metadata")
	public ResponseEntity<ApiResponse<TeacherDocumentResponse>> createDocument(
			@PathVariable UUID teacherId,
			@Valid @RequestBody TeacherDocumentRequest body,
			HttpServletRequest request) {
		return created(teacherService.createDocument(teacherId, body), "Teacher document created successfully", request);
	}

	@PutMapping("/{teacherId}/documents/{documentId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Update teacher document metadata")
	public ResponseEntity<ApiResponse<TeacherDocumentResponse>> updateDocument(
			@PathVariable UUID teacherId,
			@PathVariable UUID documentId,
			@Valid @RequestBody TeacherDocumentRequest body,
			HttpServletRequest request) {
		return ok(teacherService.updateDocument(teacherId, documentId, body), "Teacher document updated successfully", request);
	}

	@DeleteMapping("/{teacherId}/documents/{documentId}")
	@PreAuthorize("hasAuthority('TEACHERS_MANAGE')")
	@Operation(summary = "Soft delete teacher document")
	public ResponseEntity<ApiResponse<TeacherDocumentResponse>> deleteDocument(
			@PathVariable UUID teacherId,
			@PathVariable UUID documentId,
			HttpServletRequest request) {
		return ok(teacherService.deleteDocument(teacherId, documentId), "Teacher document deleted successfully", request);
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
}
