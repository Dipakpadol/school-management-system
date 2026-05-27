package com.school.erp.modules.academic.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.AssignClassTeacherRequest;
import com.school.erp.modules.academic.api.dto.AssignSubjectTeacherRequest;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Academic Hierarchy", description = "Academic year, class, division, and teacher mappings.")
public class AcademicHierarchyController {

	private final AcademicHierarchyService academicHierarchyService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get academic years")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(academicHierarchyService.getAcademicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/classes")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get classes by academic year")
	public ResponseEntity<ApiResponse<List<ClassResponse>>> classes(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getClasses(academicYearId), "Classes fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/sections")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get sections by class")
	public ResponseEntity<ApiResponse<List<SectionResponse>>> sections(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getSections(classId), "Sections fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/sections/{sectionId}/teachers")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get class/division teacher details")
	public ResponseEntity<ApiResponse<ClassSectionTeachersResponse>> teacherDetails(
			@PathVariable UUID classId,
			@PathVariable UUID sectionId,
			HttpServletRequest request) {
		return ok(
				academicHierarchyService.getTeacherDetails(classId, sectionId),
				"Class and section teacher details fetched successfully",
				request);
	}

	@PostMapping("/classes/{classId}/sections/{sectionId}/class-teacher")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Assign class teacher")
	public ResponseEntity<ApiResponse<ClassSectionTeachersResponse>> assignClassTeacher(
			@PathVariable UUID classId,
			@PathVariable UUID sectionId,
			@Valid @RequestBody AssignClassTeacherRequest body,
			HttpServletRequest request) {
		return ok(
				academicHierarchyService.assignClassTeacher(classId, sectionId, body),
				"Class teacher assigned successfully",
				request);
	}

	@PostMapping("/classes/{classId}/sections/{sectionId}/subjects/{subjectId}/teacher")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Assign subject teacher")
	public ResponseEntity<ApiResponse<ClassSectionTeachersResponse>> assignSubjectTeacher(
			@PathVariable UUID classId,
			@PathVariable UUID sectionId,
			@PathVariable UUID subjectId,
			@Valid @RequestBody AssignSubjectTeacherRequest body,
			HttpServletRequest request) {
		return ok(
				academicHierarchyService.assignSubjectTeacher(classId, sectionId, subjectId, body),
				"Subject teacher assigned successfully",
				request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
