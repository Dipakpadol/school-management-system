package com.school.erp.modules.notifications.api;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

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
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification templates, send-ready records, and notification history.")
public class NotificationsController {

	private static final String MODULE = "NOTIFICATIONS";

	private final ModuleRecordControllerSupport support;

	@GetMapping("/{recordType}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	@Operation(summary = "List notification records")
	public ResponseEntity<ApiResponse<PageResponse<ModuleRecordResponse>>> list(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return support.list(MODULE, recordType, searchRequest, pageRequest, request);
	}

	@PostMapping("/{recordType}")
	@PreAuthorize("hasAnyAuthority('NOTIFICATIONS_MANAGE','NOTIFICATIONS_SEND')")
	@Operation(summary = "Create notification record")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> create(
			@PathVariable String recordType,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.create(MODULE, recordType, body, request);
	}

	@GetMapping("/{recordType}/counts")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<ApiResponse<java.util.List<ModuleRecordCountResponse>>> counts(HttpServletRequest request) {
		return support.counts(MODULE, request);
	}

	@PostMapping(value = "/{recordType}/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importExcel(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importExcel(MODULE, recordType, file, request);
	}

	@PostMapping(value = "/{recordType}/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importCsv(
			@PathVariable String recordType,
			@RequestParam("file") MultipartFile file,
			HttpServletRequest request) {
		return support.importCsv(MODULE, recordType, file, request);
	}

	@GetMapping("/{recordType}/export/excel")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<byte[]> exportExcel(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportExcel(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/csv")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<byte[]> exportCsv(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportCsv(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/export/pdf")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<byte[]> exportPdf(
			@PathVariable String recordType,
			@Valid @ParameterObject ModuleRecordSearchRequest searchRequest) {
		return support.exportPdf(MODULE, recordType, searchRequest);
	}

	@GetMapping("/{recordType}/template")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<byte[]> template(@PathVariable String recordType) {
		return support.templateExcel(recordType);
	}

	@GetMapping("/{recordType}/template/csv")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<byte[]> templateCsv() {
		return support.templateCsv();
	}

	@GetMapping("/import-errors/{batchId}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<ApiResponse<ImportResultDto>> importErrors(
			@PathVariable UUID batchId,
			HttpServletRequest request) {
		return support.importErrors(batchId, request);
	}

	@GetMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> get(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.get(MODULE, recordType, id, request);
	}

	@PutMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	public ResponseEntity<ApiResponse<ModuleRecordResponse>> update(
			@PathVariable String recordType,
			@PathVariable UUID id,
			@Valid @RequestBody ModuleRecordRequest body,
			HttpServletRequest request) {
		return support.update(MODULE, recordType, id, body, request);
	}

	@DeleteMapping("/{recordType}/{id}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	public ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable String recordType,
			@PathVariable UUID id,
			HttpServletRequest request) {
		return support.delete(MODULE, recordType, id, request);
	}
}
