package com.school.erp.modules.portal.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceSummaryResponse;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.common.api.PageResponse;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentExamResultsResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.portal.api.dto.PortalLibrarySummaryResponse;
import com.school.erp.modules.portal.api.dto.PortalStudentDashboardResponse;
import com.school.erp.modules.portal.api.dto.PortalStudentProfileResponse;
import com.school.erp.modules.portal.api.dto.PortalTeacherDashboardResponse;
import com.school.erp.modules.portal.api.dto.PortalTeacherScopeResponse;
import com.school.erp.modules.portal.application.PortalService;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;
import com.school.erp.modules.transport.api.dto.StudentTransportAssignmentResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/portal")
@RequiredArgsConstructor
@Tag(name = "Portals", description = "Role-aware student, parent, and teacher portal APIs.")
public class PortalController {

	private final PortalService portalService;

	@GetMapping("/student/dashboard")
	@PreAuthorize("hasRole('STUDENT')")
	@Operation(summary = "Get current student portal dashboard")
	public ResponseEntity<ApiResponse<PortalStudentDashboardResponse>> studentDashboard(
			@RequestParam(required = false) UUID academicYearId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.studentDashboard(academicYearId, pageRequest), "Student portal dashboard fetched successfully", request);
	}

	@GetMapping("/student/profile")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<PortalStudentProfileResponse>> studentProfile(HttpServletRequest request) {
		return ok(portalService.studentProfile(), "Student profile fetched successfully", request);
	}

	@GetMapping("/student/attendance")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<StudentAttendanceHistoryResponse>> studentAttendance(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) AttendanceStatus status,
			@RequestParam(required = false) String sort,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				portalService.studentAttendance(academicYearId, fromDate, toDate, status, pageRequest, sort),
				"Student attendance fetched successfully",
				request);
	}

	@GetMapping("/student/attendance/summary")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> studentAttendanceSummary(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.studentAttendanceSummary(academicYearId), "Student attendance summary fetched successfully", request);
	}

	@GetMapping("/student/exams")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<StudentExamResultsResponse>> studentExams(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID examTypeId,
			@RequestParam(required = false) UUID examScheduleId,
			HttpServletRequest request) {
		return ok(portalService.studentExams(academicYearId, examTypeId, examScheduleId), "Student exams fetched successfully", request);
	}

	@GetMapping("/student/fees")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<StudentFeeSummaryResponse>> studentFees(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.studentFees(academicYearId), "Student fees fetched successfully", request);
	}

	@GetMapping("/student/library")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<PortalLibrarySummaryResponse>> studentLibrary(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.studentLibrary(pageRequest), "Student library activity fetched successfully", request);
	}

	@GetMapping("/student/hostel")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<HostelAllocationResponse>> studentHostel(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.studentHostel(academicYearId), "Student hostel allocation fetched successfully", request);
	}

	@GetMapping("/student/transport")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<StudentTransportAssignmentResponse>> studentTransport(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.studentTransport(academicYearId), "Student transport assignment fetched successfully", request);
	}

	@GetMapping("/student/communications")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<PageResponse<CommunicationResponse>>> studentCommunications(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.studentCommunications(pageRequest), "Student communications fetched successfully", request);
	}

	@GetMapping("/parent/children")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<List<StudentSummaryResponse>>> parentChildren(HttpServletRequest request) {
		return ok(portalService.parentChildren(), "Parent children fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/dashboard")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<PortalStudentDashboardResponse>> parentChildDashboard(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.parentChildDashboard(childId, academicYearId, pageRequest), "Child dashboard fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/profile")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<PortalStudentProfileResponse>> parentChildProfile(
			@PathVariable UUID childId,
			HttpServletRequest request) {
		return ok(portalService.parentChildProfile(childId), "Child profile fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/attendance")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<StudentAttendanceHistoryResponse>> parentChildAttendance(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) AttendanceStatus status,
			@RequestParam(required = false) String sort,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				portalService.parentChildAttendance(childId, academicYearId, fromDate, toDate, status, pageRequest, sort),
				"Child attendance fetched successfully",
				request);
	}

	@GetMapping("/parent/children/{childId}/attendance/summary")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> parentChildAttendanceSummary(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(
				portalService.parentChildAttendanceSummary(childId, academicYearId),
				"Child attendance summary fetched successfully",
				request);
	}

	@GetMapping("/parent/children/{childId}/exams")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<StudentExamResultsResponse>> parentChildExams(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID examTypeId,
			@RequestParam(required = false) UUID examScheduleId,
			HttpServletRequest request) {
		return ok(portalService.parentChildExams(childId, academicYearId, examTypeId, examScheduleId), "Child exams fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/fees")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<StudentFeeSummaryResponse>> parentChildFees(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.parentChildFees(childId, academicYearId), "Child fees fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/library")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<PortalLibrarySummaryResponse>> parentChildLibrary(
			@PathVariable UUID childId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.parentChildLibrary(childId, pageRequest), "Child library activity fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/hostel")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<HostelAllocationResponse>> parentChildHostel(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.parentChildHostel(childId, academicYearId), "Child hostel allocation fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/transport")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<StudentTransportAssignmentResponse>> parentChildTransport(
			@PathVariable UUID childId,
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.parentChildTransport(childId, academicYearId), "Child transport assignment fetched successfully", request);
	}

	@GetMapping("/parent/children/{childId}/communications")
	@PreAuthorize("hasRole('PARENT')")
	public ResponseEntity<ApiResponse<PageResponse<CommunicationResponse>>> parentChildCommunications(
			@PathVariable UUID childId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.parentChildCommunications(childId, pageRequest), "Child communications fetched successfully", request);
	}

	@GetMapping("/teacher/dashboard")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<PortalTeacherDashboardResponse>> teacherDashboard(
			@RequestParam(required = false) UUID academicYearId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.teacherDashboard(academicYearId, pageRequest), "Teacher portal dashboard fetched successfully", request);
	}

	@GetMapping("/teacher/profile")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<TeacherResponse>> teacherProfile(HttpServletRequest request) {
		return ok(portalService.teacherProfile(), "Teacher profile fetched successfully", request);
	}

	@GetMapping("/teacher/assignments")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<List<PortalTeacherScopeResponse>>> teacherAssignments(
			@RequestParam(required = false) UUID academicYearId,
			HttpServletRequest request) {
		return ok(portalService.teacherAssignments(academicYearId), "Teacher assignments fetched successfully", request);
	}

	@GetMapping("/teacher/students")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<List<ExamStudentResponse>>> teacherStudents(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			HttpServletRequest request) {
		return ok(portalService.teacherStudents(academicYearId, classId, sectionId), "Teacher scoped students fetched successfully", request);
	}

	@GetMapping("/teacher/attendance/daily")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<DailyAttendanceResponse>> teacherDailyAttendance(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			@RequestParam("date") LocalDate date,
			HttpServletRequest request) {
		return ok(portalService.teacherDailyAttendance(academicYearId, classId, sectionId, date), "Teacher attendance fetched successfully", request);
	}

	@PostMapping("/teacher/attendance/daily")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<DailyAttendanceResponse>> teacherSaveDaily(
			@Valid @RequestBody DailyAttendanceRequest body,
			HttpServletRequest request) {
		return ok(portalService.teacherSaveDaily(body), "Teacher attendance saved successfully", request);
	}

	@GetMapping("/teacher/marks")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<MarksEntryResponse>> teacherMarks(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			@RequestParam UUID examScheduleId,
			@RequestParam UUID subjectId,
			HttpServletRequest request) {
		return ok(
				portalService.teacherMarks(academicYearId, classId, sectionId, examScheduleId, subjectId),
				"Teacher marks fetched successfully",
				request);
	}

	@PostMapping("/teacher/marks")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<MarksEntryResponse>> teacherSaveMarks(
			@Valid @RequestBody MarksEntryRequest body,
			HttpServletRequest request) {
		return ok(portalService.teacherSaveMarks(body), "Teacher marks saved successfully", request);
	}

	@GetMapping("/teacher/communications")
	@PreAuthorize("hasRole('TEACHER')")
	public ResponseEntity<ApiResponse<PageResponse<CommunicationResponse>>> teacherCommunications(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(portalService.teacherCommunications(pageRequest), "Teacher communications fetched successfully", request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
