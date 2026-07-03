package com.school.erp.modules.attendance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.attendance.api.dto.TeacherAttendanceTeacherResponse;
import com.school.erp.modules.attendance.api.dto.TeacherDailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.TeacherDailyAttendanceResponse;
import com.school.erp.modules.attendance.application.TeacherAttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/teacher-attendance")
@RequiredArgsConstructor
@Tag(name = "Teacher Attendance", description = "Academic-year-wise teacher daily attendance.")
public class TeacherAttendanceController {

	private final AcademicHierarchyService academicHierarchyService;
	private final TeacherAttendanceService teacherAttendanceService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','TEACHERS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get academic years for teacher attendance")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(academicHierarchyService.getAcademicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping("/teachers")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','TEACHERS_READ')")
	@Operation(summary = "Get teachers for attendance marking")
	public ResponseEntity<ApiResponse<List<TeacherAttendanceTeacherResponse>>> teachers(
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(
				teacherAttendanceService.getTeachers(academicYearId),
				"Teachers fetched successfully",
				request);
	}

	@PostMapping("/daily")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_MARK','TEACHERS_MANAGE')")
	@Operation(summary = "Save teacher daily attendance")
	public ResponseEntity<ApiResponse<TeacherDailyAttendanceResponse>> saveDaily(
			@Valid @RequestBody TeacherDailyAttendanceRequest body,
			HttpServletRequest request) {
		return ok(teacherAttendanceService.saveDaily(body), "Teacher attendance saved successfully", request);
	}

	@GetMapping("/daily")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','TEACHERS_READ')")
	@Operation(summary = "Get teacher daily attendance")
	public ResponseEntity<ApiResponse<TeacherDailyAttendanceResponse>> daily(
			@RequestParam UUID academicYearId,
			@RequestParam("date") LocalDate date,
			HttpServletRequest request) {
		return ok(
				teacherAttendanceService.getDaily(academicYearId, date),
				"Teacher attendance fetched successfully",
				request);
	}

	@GetMapping("/export")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','TEACHERS_READ')")
	@Operation(summary = "Export teacher attendance CSV")
	public ResponseEntity<byte[]> export(
			@RequestParam UUID academicYearId,
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"teacher-attendance.csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(teacherAttendanceService.export(academicYearId, fromDate, toDate));
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
