package com.school.erp.modules.backup.api;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.backup.api.dto.BackupRecordResponse;
import com.school.erp.modules.backup.api.dto.BackupSummaryResponse;
import com.school.erp.modules.backup.api.dto.CreateBackupRequest;
import com.school.erp.modules.backup.api.dto.RestoreBackupRequest;
import com.school.erp.modules.backup.api.dto.RestoreHistoryResponse;
import com.school.erp.modules.backup.application.BackupDownload;
import com.school.erp.modules.backup.application.BackupService;
import com.school.erp.modules.backup.domain.BackupStatus;
import com.school.erp.modules.backup.domain.BackupType;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/v1/backups")
@RequiredArgsConstructor
@Tag(name = "Backup & Restore", description = "Database backup, secure download, restore, and retention operations.")
public class BackupController {

	private final BackupService backupService;

	@GetMapping("/summary")
	@PreAuthorize("hasAuthority('BACKUP_READ')")
	public ResponseEntity<ApiResponse<BackupSummaryResponse>> summary(HttpServletRequest request) {
		return ok(backupService.summary(), "Backup summary fetched successfully", request);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('BACKUP_READ')")
	public ResponseEntity<ApiResponse<PageResponse<BackupRecordResponse>>> backups(
			@RequestParam(required = false) BackupStatus status,
			@RequestParam(required = false) BackupType backupType,
			@RequestParam(required = false) String keyword,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(backupService.backups(status, backupType, keyword, pageRequest), "Backups fetched successfully", request);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('BACKUP_CREATE')")
	public ResponseEntity<ApiResponse<BackupRecordResponse>> createBackup(
			@Valid @RequestBody(required = false) CreateBackupRequest body,
			HttpServletRequest request) {
		return created(backupService.createBackup(body), "Backup created successfully", request);
	}

	@GetMapping("/{backupId}/download")
	@PreAuthorize("hasAuthority('BACKUP_DOWNLOAD')")
	public ResponseEntity<InputStreamResource> download(@PathVariable UUID backupId) throws IOException {
		BackupDownload download = backupService.download(backupId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(download.contentType()))
				.contentLength(download.sizeBytes())
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(download.fileName()).build().toString())
				.body(new InputStreamResource(Files.newInputStream(download.path())));
	}

	@PostMapping("/{backupId}/restore")
	@PreAuthorize("hasAuthority('BACKUP_RESTORE')")
	public ResponseEntity<ApiResponse<RestoreHistoryResponse>> restore(
			@PathVariable UUID backupId,
			@Valid @RequestBody RestoreBackupRequest body,
			HttpServletRequest request) {
		return ok(backupService.restore(backupId, body), "Restore completed successfully", request);
	}

	@DeleteMapping("/{backupId}")
	@PreAuthorize("hasAuthority('BACKUP_DELETE')")
	public ResponseEntity<ApiResponse<BackupRecordResponse>> deleteBackup(
			@PathVariable UUID backupId,
			HttpServletRequest request) {
		return ok(backupService.deleteBackup(backupId), "Backup deleted successfully", request);
	}

	@GetMapping("/restores")
	@PreAuthorize("hasAuthority('BACKUP_READ')")
	public ResponseEntity<ApiResponse<PageResponse<RestoreHistoryResponse>>> restores(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(backupService.restoreHistory(pageRequest), "Restore history fetched successfully", request);
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
