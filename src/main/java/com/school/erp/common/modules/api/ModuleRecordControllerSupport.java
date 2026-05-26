package com.school.erp.common.modules.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.importexport.ImportResultDto;
import com.school.erp.common.modules.api.dto.ModuleRecordCountResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordRequest;
import com.school.erp.common.modules.api.dto.ModuleRecordResponse;
import com.school.erp.common.modules.api.dto.ModuleRecordSearchRequest;
import com.school.erp.common.modules.application.ModuleRecordService;
import com.school.erp.common.web.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModuleRecordControllerSupport {

	private final ModuleRecordService moduleRecordService;

	public ResponseEntity<ApiResponse<PageResponse<ModuleRecordResponse>>> list(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest,
			PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(moduleRecordService.search(moduleName, recordType, searchRequest, pageRequest),
				"Records fetched successfully",
				request);
	}

	public ResponseEntity<ApiResponse<ModuleRecordResponse>> get(
			String moduleName,
			String recordType,
			UUID id,
			HttpServletRequest request) {
		return ok(moduleRecordService.get(moduleName, recordType, id), "Record fetched successfully", request);
	}

	public ResponseEntity<ApiResponse<ModuleRecordResponse>> create(
			String moduleName,
			String recordType,
			ModuleRecordRequest body,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(response(moduleRecordService.create(moduleName, recordType, body), "Record created successfully", request));
	}

	public ResponseEntity<ApiResponse<ModuleRecordResponse>> update(
			String moduleName,
			String recordType,
			UUID id,
			ModuleRecordRequest body,
			HttpServletRequest request) {
		return ok(moduleRecordService.update(moduleName, recordType, id, body), "Record updated successfully", request);
	}

	public ResponseEntity<ApiResponse<Void>> delete(
			String moduleName,
			String recordType,
			UUID id,
			HttpServletRequest request) {
		moduleRecordService.delete(moduleName, recordType, id);
		return ok(null, "Record deleted successfully", request);
	}

	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			String moduleName,
			String recordType,
			MultipartFile file,
			HttpServletRequest request) {
		return ok(moduleRecordService.importExcel(moduleName, recordType, file), "Excel import completed", request);
	}

	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			String moduleName,
			String recordType,
			MultipartFile file,
			HttpServletRequest request) {
		return ok(moduleRecordService.importCsv(moduleName, recordType, file), "CSV import completed", request);
	}

	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(UUID batchId, HttpServletRequest request) {
		return ok(moduleRecordService.importErrors(batchId), "Import errors fetched successfully", request);
	}

	public ResponseEntity<byte[]> exportExcel(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest) {
		return file(moduleRecordService.exportExcel(moduleName, recordType, searchRequest), filename(moduleName, recordType, "xlsx"),
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	public ResponseEntity<byte[]> exportCsv(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest) {
		return file(moduleRecordService.exportCsv(moduleName, recordType, searchRequest), filename(moduleName, recordType, "csv"), "text/csv");
	}

	public ResponseEntity<byte[]> exportPdf(
			String moduleName,
			String recordType,
			ModuleRecordSearchRequest searchRequest) {
		return file(moduleRecordService.exportPdf(moduleName, recordType, searchRequest), filename(moduleName, recordType, "pdf"), "application/pdf");
	}

	public ResponseEntity<byte[]> templateExcel(String recordType) {
		return file(moduleRecordService.excelTemplate(recordType), "template-" + recordType + ".xlsx",
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	public ResponseEntity<byte[]> templateCsv() {
		return file(moduleRecordService.csvTemplate(), "template.csv", "text/csv");
	}

	public ResponseEntity<ApiResponse<java.util.List<ModuleRecordCountResponse>>> counts(String moduleName, HttpServletRequest request) {
		return ok(moduleRecordService.counts(moduleName), "Module counts fetched successfully", request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(response(data, message, request));
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

	private String filename(String moduleName, String recordType, String extension) {
		return moduleName.toLowerCase() + "-" + recordType.toLowerCase() + "." + extension;
	}
}
