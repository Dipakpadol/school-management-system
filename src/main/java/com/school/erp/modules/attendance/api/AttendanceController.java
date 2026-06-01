package com.school.erp.modules.attendance.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.modules.api.ModuleRecordControllerSupport;
import com.school.erp.common.modules.api.dto.ModuleRecordCountResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordRequest;
import com.school.erp.common.modules.api.dto.ModuleRecordResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordSearchRequest;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.attendance.api.dto.AttendanceStudentResponse;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceSummaryResponse;
import com.school.erp.modules.attendance.application.AttendanceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springdoc.core.annotations.ParameterObject;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Daily attendance registers and monthly attendance records.")
public class AttendanceController {

	private static final String MODULE = "ATTENDANCE";

	private final ModuleRecordControllerSupport support;
	private final AcademicHierarchyService academicHierarchyService;
	private final AttendanceService attendanceService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','ACADEMIC_READ')")
	@Operation(summary = "Get academic years for attendance")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(academicHierarchyService.getAcademicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/classes")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','ACADEMIC_READ')")
	@Operation(summary = "Get classes by academic year for attendance")
	public ResponseEntity<ApiResponse<List<ClassResponse>>> classes(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getClasses(academicYearId), "Classes fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/sections")
	@PreAuthorize("hasAnyAuthority('ATTENDANCE_READ','ACADEMIC_READ')")
	@Operation(summary = "Get sections by class for attendance")
	public ResponseEntity<ApiResponse<List<SectionResponse>>> sections(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getSections(classId), "Sections fetched successfully", request);
	}

	@GetMapping("/students")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	@Operation(summary = "Get students by academic year, class, and section")
	public ResponseEntity<ApiResponse<List<AttendanceStudentResponse>>> students(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			HttpServletRequest request) {
		return ok(
				attendanceService.getStudents(academicYearId, classId, sectionId),
				"Attendance students fetched successfully",
				request);
	}

	@PostMapping("/daily")
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	@Operation(summary = "Save daily attendance")
	public ResponseEntity<ApiResponse<DailyAttendanceResponse>> saveDaily(
			@Valid @RequestBody DailyAttendanceRequest body,
			HttpServletRequest request) {
		return ok(attendanceService.saveDaily(body), "Attendance saved successfully", request);
	}

	@GetMapping("/daily")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	@Operation(summary = "Get daily attendance")
	public ResponseEntity<ApiResponse<DailyAttendanceResponse>> daily(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			@RequestParam("date") LocalDate date,
			HttpServletRequest request) {
		return ok(
				attendanceService.getDaily(academicYearId, classId, sectionId, date),
				"Attendance fetched successfully",
				request);
	}

	@GetMapping("/students/{studentId}/summary")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	@Operation(summary = "Get student attendance summary")
	public ResponseEntity<ApiResponse<StudentAttendanceSummaryResponse>> studentSummary(
			@PathVariable UUID studentId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(
				attendanceService.getStudentSummary(studentId, academicYearId),
				"Student attendance summary fetched successfully",
				request);
	}

	@GetMapping("/export")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	@Operation(summary = "Export attendance CSV")
	public ResponseEntity<byte[]> export(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance.csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(attendanceService.export(academicYearId, classId, sectionId, fromDate, toDate));
	}

	@GetMapping("/{recordType}")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	@Operation(summary = "List attendance records")
	public ResponseEntity<ApiResponse<PageResponse<ModuleRecordResponse>>> list(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return support.list(MODULE, recordType, searchRequest, pageRequest, request);
	}

	@PostMapping("/{recordType}")
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	@Operation(summary = "Create attendance record")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> create(
			@PathVariable String recordType,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.create(MODULE, recordType, body, request);
	}

	@GetMapping("/{recordType}/counts")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<java.util.List<ModuleRecordCountResponse>>> counts(HttpServletRequest request) {
		return support.counts(MODULE, request);
	}

	@PostMapping(value = "/{recordType}/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importExcel(MODULE, recordType, file, request);
	}

	@PostMapping(value = "/{recordType}/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importCsv(MODULE, recordType, file, request);
	}

	@GetMapping("/{recordType}/export/excel")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<byte[]> exportExcel(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportExcel(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/csv")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<byte[]> exportCsv(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportCsv(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/pdf")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<byte[]> exportPdf(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportPdf(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/template")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<byte[]> template(@PathVariable String recordType) {
		return support.templateExcel(recordType);
	}

	@GetMapping("/{recordType}/template/csv")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<byte[]> templateCsv() {
		return support.templateCsv();
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest request) {
		return support.importErrors(batchId, request);
	}

	@GetMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> get(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.get(MODULE, recordType, id, request);
	}

	@PutMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> update(
			@PathVariable String recordType,
			@PathVariable UUID id,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.update(MODULE, recordType, id, body, request);
	}

	@DeleteMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	public ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.delete(MODULE, recordType, id, request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
