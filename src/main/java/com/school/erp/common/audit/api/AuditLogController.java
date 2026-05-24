package com.school.erp.common.audit.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.api.dto.AuditLogDto;
import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.web.CorrelationIdFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "System audit trail for critical ERP operations.")
public class AuditLogController {

	private final AuditLogService auditLogService;

	@GetMapping
	@PreAuthorize("hasAuthority('AUDIT_LOGS_READ')")
	@Operation(summary = "Search audit logs", description = "Searches audit events with filters, pagination, and sorting.")
	public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> searchAuditLogs(
			@Valid @ParameterObject AuditLogSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(auditLogService.search(searchRequest, pageRequest), "Audit logs fetched successfully", httpRequest);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('AUDIT_LOGS_READ')")
	@Operation(summary = "Get audit log by ID")
	public ResponseEntity<ApiResponse<AuditLogDto>> getAuditLog(
			@Parameter(description = "Audit log UUID") @PathVariable UUID id,
			HttpServletRequest httpRequest) {
		return ok(auditLogService.getById(id), "Audit log fetched successfully", httpRequest);
	}

	@GetMapping("/by-module/{moduleName}")
	@PreAuthorize("hasAuthority('AUDIT_LOGS_READ')")
	@Operation(summary = "Get audit logs by module")
	public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getByModule(
			@PathVariable String moduleName,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(auditLogService.findByModule(moduleName, pageRequest), "Audit logs fetched successfully", httpRequest);
	}

	@GetMapping("/by-entity/{entityName}/{entityId}")
	@PreAuthorize("hasAuthority('AUDIT_LOGS_READ')")
	@Operation(summary = "Get audit logs by entity")
	public ResponseEntity<ApiResponse<PageResponse<AuditLogDto>>> getByEntity(
			@PathVariable String entityName,
			@PathVariable String entityId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(auditLogService.findByEntity(entityName, entityId, pageRequest), "Audit logs fetched successfully", httpRequest);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
