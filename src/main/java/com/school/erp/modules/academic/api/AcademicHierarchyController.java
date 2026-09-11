package com.school.erp.modules.academic.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearRequest;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.AssignClassTeacherRequest;
import com.school.erp.modules.academic.api.dto.AssignSubjectTeacherRequest;
import com.school.erp.modules.academic.api.dto.ClassRequest;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.ClassSectionTeachersResponse;
import com.school.erp.modules.academic.api.dto.DivisionRequest;
import com.school.erp.modules.academic.api.dto.DivisionResponse;
import com.school.erp.modules.academic.api.dto.DivisionSubjectRequest;
import com.school.erp.modules.academic.api.dto.DivisionSubjectResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.api.dto.SubjectResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

	@PostMapping("/academic-years")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Create academic year")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> createAcademicYear(
			@Valid @RequestBody AcademicYearRequest body,
			HttpServletRequest request) {
		return created(academicHierarchyService.createAcademicYear(body), "Academic year created successfully", request);
	}

	@GetMapping("/academic-years/current")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get current academic year")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> currentAcademicYear(HttpServletRequest request) {
		return ok(academicHierarchyService.getCurrentAcademicYear(), "Current academic year fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get academic year")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> academicYear(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getAcademicYear(academicYearId), "Academic year fetched successfully", request);
	}

	@PutMapping("/academic-years/{academicYearId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Update academic year")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> updateAcademicYear(
			@PathVariable UUID academicYearId,
			@Valid @RequestBody AcademicYearRequest body,
			HttpServletRequest request) {
		return ok(academicHierarchyService.updateAcademicYear(academicYearId, body), "Academic year updated successfully", request);
	}

	@PatchMapping("/academic-years/{academicYearId}/current")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Mark academic year current")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> setCurrentAcademicYear(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.setCurrentAcademicYear(academicYearId), "Academic year marked current successfully", request);
	}

	@DeleteMapping("/academic-years/{academicYearId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Soft delete academic year")
	public ResponseEntity<ApiResponse<AcademicYearResponse>> deleteAcademicYear(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.deleteAcademicYear(academicYearId), "Academic year deleted successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/classes")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get classes by academic year")
	public ResponseEntity<ApiResponse<List<ClassResponse>>> classes(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getClasses(academicYearId), "Classes fetched successfully", request);
	}

	@PostMapping("/academic-years/{academicYearId}/classes")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Create class under academic year")
	public ResponseEntity<ApiResponse<ClassResponse>> createClass(
			@PathVariable UUID academicYearId,
			@Valid @RequestBody ClassRequest body,
			HttpServletRequest request) {
		return created(academicHierarchyService.createClass(academicYearId, body), "Class created successfully", request);
	}

	@GetMapping("/classes/{classId}")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get class")
	public ResponseEntity<ApiResponse<ClassResponse>> classDetails(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getClass(classId), "Class fetched successfully", request);
	}

	@PutMapping("/classes/{classId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Update class")
	public ResponseEntity<ApiResponse<ClassResponse>> updateClass(
			@PathVariable UUID classId,
			@Valid @RequestBody ClassRequest body,
			HttpServletRequest request) {
		return ok(academicHierarchyService.updateClass(classId, body), "Class updated successfully", request);
	}

	@DeleteMapping("/classes/{classId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Soft delete class")
	public ResponseEntity<ApiResponse<ClassResponse>> deleteClass(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.deleteClass(classId), "Class deleted successfully", request);
	}

	@GetMapping("/classes/{classId}/sections")
	@PreAuthorize("hasAnyAuthority('STUDENTS_READ', 'ACADEMIC_READ')")
	@Operation(summary = "Get sections by class")
	public ResponseEntity<ApiResponse<List<SectionResponse>>> sections(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getSections(classId), "Sections fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/divisions")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get divisions by class")
	public ResponseEntity<ApiResponse<List<DivisionResponse>>> divisions(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getDivisions(classId), "Divisions fetched successfully", request);
	}

	@PostMapping("/classes/{classId}/divisions")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Create division under class")
	public ResponseEntity<ApiResponse<DivisionResponse>> createDivision(
			@PathVariable UUID classId,
			@Valid @RequestBody DivisionRequest body,
			HttpServletRequest request) {
		return created(academicHierarchyService.createDivision(classId, body), "Division created successfully", request);
	}

	@GetMapping("/divisions/{divisionId}")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get division")
	public ResponseEntity<ApiResponse<DivisionResponse>> division(
			@PathVariable UUID divisionId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getDivision(divisionId), "Division fetched successfully", request);
	}

	@PutMapping("/divisions/{divisionId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Update division")
	public ResponseEntity<ApiResponse<DivisionResponse>> updateDivision(
			@PathVariable UUID divisionId,
			@Valid @RequestBody DivisionRequest body,
			HttpServletRequest request) {
		return ok(academicHierarchyService.updateDivision(divisionId, body), "Division updated successfully", request);
	}

	@DeleteMapping("/divisions/{divisionId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Soft delete division")
	public ResponseEntity<ApiResponse<DivisionResponse>> deleteDivision(
			@PathVariable UUID divisionId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.deleteDivision(divisionId), "Division deleted successfully", request);
	}

	@GetMapping("/subjects")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get subjects")
	public ResponseEntity<ApiResponse<List<SubjectResponse>>> subjects(HttpServletRequest request) {
		return ok(academicHierarchyService.getSubjects(), "Subjects fetched successfully", request);
	}

	@GetMapping("/divisions/{divisionId}/subjects")
	@PreAuthorize("hasAuthority('ACADEMIC_READ')")
	@Operation(summary = "Get division subjects")
	public ResponseEntity<ApiResponse<List<DivisionSubjectResponse>>> divisionSubjects(
			@PathVariable UUID divisionId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getDivisionSubjects(divisionId), "Division subjects fetched successfully", request);
	}

	@PostMapping("/divisions/{divisionId}/subjects")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Assign subject to division")
	public ResponseEntity<ApiResponse<DivisionSubjectResponse>> addDivisionSubject(
			@PathVariable UUID divisionId,
			@Valid @RequestBody DivisionSubjectRequest body,
			HttpServletRequest request) {
		return created(academicHierarchyService.addDivisionSubject(divisionId, body), "Division subject added successfully", request);
	}

	@PutMapping("/divisions/{divisionId}/subjects/{divisionSubjectId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Update division subject teacher")
	public ResponseEntity<ApiResponse<DivisionSubjectResponse>> updateDivisionSubject(
			@PathVariable UUID divisionId,
			@PathVariable UUID divisionSubjectId,
			@Valid @RequestBody DivisionSubjectRequest body,
			HttpServletRequest request) {
		return ok(academicHierarchyService.updateDivisionSubject(divisionId, divisionSubjectId, body), "Division subject updated successfully", request);
	}

	@DeleteMapping("/divisions/{divisionId}/subjects/{divisionSubjectId}")
	@PreAuthorize("hasAuthority('ACADEMIC_MANAGE')")
	@Operation(summary = "Remove subject from division")
	public ResponseEntity<ApiResponse<DivisionSubjectResponse>> deleteDivisionSubject(
			@PathVariable UUID divisionId,
			@PathVariable UUID divisionSubjectId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.deleteDivisionSubject(divisionId, divisionSubjectId), "Division subject removed successfully", request);
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

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
