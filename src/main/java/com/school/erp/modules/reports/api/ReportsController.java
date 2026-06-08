package com.school.erp.modules.reports.api;

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
import com.school.erp.common.modules.application.ModuleRecordService;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse;
import com.school.erp.modules.reports.application.ReportsService;
import com.school.erp.modules.reports.application.ReportsService.ReportExportFile;
import com.school.erp.modules.reports.application.ReportsService.ReportExportRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Report catalog, operational summaries, and export-ready report definitions.")
public class ReportsController {

	private static final String MODULE = "REPORTS";
	private static final List<String> REPORT_MODULES = List.of(
			"ACADEMIC",
			"HOSTEL",
			"ATTENDANCE",
			"EXAMS",
			"NOTIFICATIONS",
			"SETTINGS",
			"REPORTS");

	private final ModuleRecordControllerSupport support;
	private final ModuleRecordService moduleRecordService;
	private final ReportsService reportsService;

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	@Operation(summary = "Get operational module summary")
	public ResponseEntity<ApiResponse<List<ReportModuleSummary>>> summary(HttpServletRequest request) {
		List<ReportModuleSummary> summaries = REPORT_MODULES.stream()
				.map(module -> new ReportModuleSummary(module, moduleRecordService.counts(module)))
				.toList();
		return ResponseEntity.ok(ApiResponse.success(
				summaries,
				"Report summary fetched successfully",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}

	@GetMapping("/options")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	@Operation(summary = "Get report export options")
	public ResponseEntity<ApiResponse<ReportOptionsResponse>> options(HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				reportsService.options(),
				"Report options fetched successfully",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}

	@GetMapping("/export")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	@Operation(summary = "Export a selected report")
	public ResponseEntity<byte[]> export(@Valid @ParameterObject ReportExportRequest exportRequest) {
		ReportExportFile file = reportsService.export(exportRequest);
		return ResponseEntity.ok()
				.header("Content-Disposition", "attachment; filename=\"" + file.filename() + "\"")
				.contentType(MediaType.parseMediaType(file.contentType()))
				.body(file.content());
	}

	@GetMapping("/{recordType}")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	@Operation(summary = "List report records")
	public ResponseEntity<ApiResponse<PageResponse<ModuleRecordResponse>>> list(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return support.list(MODULE, recordType, searchRequest, pageRequest, request);
	}

	@PostMapping("/{recordType}")
	@PreAuthorize("hasAuthority('REPORTS_MANAGE')")
	@Operation(summary = "Create report record")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> create(
			@PathVariable String recordType,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.create(MODULE, recordType, body, request);
	}

	@PostMapping(value = "/{recordType}/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('REPORTS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importExcel(MODULE, recordType, file, request);
	}

	@PostMapping(value = "/{recordType}/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('REPORTS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importCsv(MODULE, recordType, file, request);
	}

	@GetMapping("/{recordType}/export/excel")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<byte[]> exportExcel(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportExcel(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/csv")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<byte[]> exportCsv(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportCsv(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/pdf")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<byte[]> exportPdf(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportPdf(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/template")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<byte[]> template(@PathVariable String recordType) {
		return support.templateExcel(recordType);
	}

	@GetMapping("/{recordType}/template/csv")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<byte[]> templateCsv() {
		return support.templateCsv();
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest request) {
		return support.importErrors(batchId, request);
	}

	@GetMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('REPORTS_READ')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> get(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.get(MODULE, recordType, id, request);
	}

	@PutMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('REPORTS_MANAGE')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> update(
			@PathVariable String recordType,
			@PathVariable UUID id,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.update(MODULE, recordType, id, body, request);
	}

	@DeleteMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('REPORTS_MANAGE')")
	public ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.delete(MODULE, recordType, id, request);
	}

	public record ReportModuleSummary(String moduleName, List<ModuleRecordCountResponse> counts) {
	}
}
