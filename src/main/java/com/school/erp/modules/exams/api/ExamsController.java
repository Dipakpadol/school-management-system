package com.school.erp.modules.exams.api;

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
import com.school.erp.modules.academic.api.dto.DivisionSubjectResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.exams.api.dto.ExamScheduleRequest;
import com.school.erp.modules.exams.api.dto.ExamScheduleResponse;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.ExamTypeRequest;
import com.school.erp.modules.exams.api.dto.ExamTypeResponse;
import com.school.erp.modules.exams.api.dto.GenerateResultRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentResultResponse;
import com.school.erp.modules.exams.application.ExamService;

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
@RequestMapping("/v1/exams")
@RequiredArgsConstructor
@Tag(name = "Exams & Results", description = "Exam types, schedules, grade setup, marks, and result records.")
public class ExamsController {

	private static final String MODULE = "EXAMS";

	private final ModuleRecordControllerSupport support;
	private final AcademicHierarchyService academicHierarchyService;
	private final ExamService examService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAnyAuthority('EXAMS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get academic years for exams")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(academicHierarchyService.getAcademicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/classes")
	@PreAuthorize("hasAnyAuthority('EXAMS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get classes by academic year for exams")
	public ResponseEntity<ApiResponse<List<ClassResponse>>> classes(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getClasses(academicYearId), "Classes fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/sections")
	@PreAuthorize("hasAnyAuthority('EXAMS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get sections by class for exams")
	public ResponseEntity<ApiResponse<List<SectionResponse>>> sections(
			@PathVariable UUID classId,
			HttpServletRequest request) {
		return ok(academicHierarchyService.getSections(classId), "Sections fetched successfully", request);
	}

	@GetMapping("/classes/{classId}/sections/{sectionId}/subjects")
	@PreAuthorize("hasAnyAuthority('EXAMS_READ','ACADEMIC_READ')")
	@Operation(summary = "Get subjects by class and section for exams")
	public ResponseEntity<ApiResponse<List<DivisionSubjectResponse>>> subjects(
			@PathVariable UUID classId,
			@PathVariable UUID sectionId,
			HttpServletRequest request) {
		return ok(examService.getSubjects(classId, sectionId), "Subjects fetched successfully", request);
	}

	@GetMapping("/students")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Get students by academic year, class, and section for exams")
	public ResponseEntity<ApiResponse<List<ExamStudentResponse>>> students(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			HttpServletRequest request) {
		return ok(examService.getStudents(academicYearId, classId, sectionId), "Students fetched successfully", request);
	}

	@PostMapping("/types")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Create exam type")
	public ResponseEntity<ApiResponse<ExamTypeResponse>> createType(
			@Valid @RequestBody ExamTypeRequest body,
			HttpServletRequest request) {
		return created(examService.createType(body), "Exam type created successfully", request);
	}

	@PutMapping("/types/{examTypeId}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Update exam type")
	public ResponseEntity<ApiResponse<ExamTypeResponse>> updateType(
			@PathVariable UUID examTypeId,
			@Valid @RequestBody ExamTypeRequest body,
			HttpServletRequest request) {
		return ok(examService.updateType(examTypeId, body), "Exam type updated successfully", request);
	}

	@GetMapping("/types/{examTypeId}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Get exam type")
	public ResponseEntity<ApiResponse<ExamTypeResponse>> getType(
			@PathVariable UUID examTypeId,
			HttpServletRequest request) {
		return ok(examService.getType(examTypeId), "Exam type fetched successfully", request);
	}

	@GetMapping("/types")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "List exam types")
	public ResponseEntity<ApiResponse<List<ExamTypeResponse>>> getTypes(HttpServletRequest request) {
		return ok(examService.getTypes(), "Exam types fetched successfully", request);
	}

	@DeleteMapping("/types/{examTypeId}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Delete exam type")
	public ResponseEntity<ApiResponse<Void>> deleteType(
			@PathVariable UUID examTypeId,
			HttpServletRequest request) {
		examService.deleteType(examTypeId);
		return ok(null, "Exam type deleted successfully", request);
	}

	@PostMapping("/schedules")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Create exam schedule")
	public ResponseEntity<ApiResponse<ExamScheduleResponse>> createSchedule(
			@Valid @RequestBody ExamScheduleRequest body,
			HttpServletRequest request) {
		return created(examService.createSchedule(body), "Exam schedule created successfully", request);
	}

	@PutMapping("/schedules/{examScheduleId}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Update exam schedule")
	public ResponseEntity<ApiResponse<ExamScheduleResponse>> updateSchedule(
			@PathVariable UUID examScheduleId,
			@Valid @RequestBody ExamScheduleRequest body,
			HttpServletRequest request) {
		return ok(examService.updateSchedule(examScheduleId, body), "Exam schedule updated successfully", request);
	}

	@GetMapping("/schedules/{examScheduleId}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Get exam schedule")
	public ResponseEntity<ApiResponse<ExamScheduleResponse>> getSchedule(
			@PathVariable UUID examScheduleId,
			HttpServletRequest request) {
		return ok(examService.getSchedule(examScheduleId), "Exam schedule fetched successfully", request);
	}

	@GetMapping("/schedules")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "List exam schedules")
	public ResponseEntity<ApiResponse<List<ExamScheduleResponse>>> getSchedules(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID classId,
			@RequestParam(required = false) UUID sectionId,
			HttpServletRequest request) {
		return ok(examService.getSchedules(academicYearId, classId, sectionId), "Exam schedules fetched successfully", request);
	}

	@DeleteMapping("/schedules/{examScheduleId}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Delete exam schedule")
	public ResponseEntity<ApiResponse<Void>> deleteSchedule(
			@PathVariable UUID examScheduleId,
			HttpServletRequest request) {
		examService.deleteSchedule(examScheduleId);
		return ok(null, "Exam schedule deleted successfully", request);
	}

	@PostMapping("/marks")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Save marks")
	public ResponseEntity<ApiResponse<MarksEntryResponse>> saveMarks(
			@Valid @RequestBody MarksEntryRequest body,
			HttpServletRequest request) {
		return ok(examService.saveMarks(body), "Marks saved successfully", request);
	}

	@GetMapping("/marks")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Get marks")
	public ResponseEntity<ApiResponse<MarksEntryResponse>> getMarks(
			@RequestParam UUID academicYearId,
			@RequestParam UUID classId,
			@RequestParam UUID sectionId,
			@RequestParam UUID examScheduleId,
			@RequestParam UUID subjectId,
			HttpServletRequest request) {
		return ok(
				examService.getMarks(academicYearId, classId, sectionId, examScheduleId, subjectId),
				"Marks fetched successfully",
				request);
	}

	@PostMapping("/results/generate")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Generate results")
	public ResponseEntity<ApiResponse<List<StudentResultResponse>>> generateResults(
			@Valid @RequestBody GenerateResultRequest body,
			HttpServletRequest request) {
		return ok(examService.generateResults(body), "Results generated successfully", request);
	}

	@GetMapping("/results/students/{studentId}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Get student result")
	public ResponseEntity<ApiResponse<StudentResultResponse>> studentResult(
			@PathVariable UUID studentId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(examService.getStudentResult(studentId, academicYearId), "Student result fetched successfully", request);
	}

	@GetMapping("/results/students/{studentId}/report-card")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "Export report card")
	public ResponseEntity<byte[]> reportCard(
			@PathVariable UUID studentId,
			@RequestParam UUID academicYearId) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-card.csv\"")
				.contentType(MediaType.parseMediaType("text/csv"))
				.body(examService.exportReportCard(studentId, academicYearId));
	}

	@GetMapping("/{recordType}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	@Operation(summary = "List exam records")
	public ResponseEntity<ApiResponse<PageResponse<ModuleRecordResponse>>> list(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return support.list(MODULE, recordType, searchRequest, pageRequest, request);
	}

	@PostMapping("/{recordType}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	@Operation(summary = "Create exam record")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> create(
			@PathVariable String recordType,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.create(MODULE, recordType, body, request);
	}

	@GetMapping("/{recordType}/counts")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<ApiResponse<java.util.List<ModuleRecordCountResponse>>> counts(HttpServletRequest request) {
		return support.counts(MODULE, request);
	}

	@PostMapping(value = "/{recordType}/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importExcel(MODULE, recordType, file, request);
	}

	@PostMapping(value = "/{recordType}/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importCsv(MODULE, recordType, file, request);
	}

	@GetMapping("/{recordType}/export/excel")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<byte[]> exportExcel(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportExcel(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/csv")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<byte[]> exportCsv(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportCsv(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/pdf")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<byte[]> exportPdf(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportPdf(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/template")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<byte[]> template(@PathVariable String recordType) {
		return support.templateExcel(recordType);
	}

	@GetMapping("/{recordType}/template/csv")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<byte[]> templateCsv() {
		return support.templateCsv();
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest request) {
		return support.importErrors(batchId, request);
	}

	@GetMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('EXAMS_READ')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> get(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.get(MODULE, recordType, id, request);
	}

	@PutMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> update(
			@PathVariable String recordType,
			@PathVariable UUID id,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.update(MODULE, recordType, id, body, request);
	}

	@DeleteMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('EXAMS_MANAGE')")
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

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(201).body(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
