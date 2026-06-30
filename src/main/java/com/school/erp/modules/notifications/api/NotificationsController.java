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
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.notifications.api.dto.NotificationLogResponse;
import com.school.erp.modules.notifications.api.dto.NotificationTemplateRequest;
import com.school.erp.modules.notifications.api.dto.NotificationTemplateResponse;
import com.school.erp.modules.notifications.api.dto.TestEmailNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestSmsNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestWhatsAppNotificationRequest;
import com.school.erp.modules.notifications.application.NotificationService;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
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
	private final NotificationService notificationService;

	@PostMapping("/test/email")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_SEND')")
	@Operation(summary = "Send test email notification")
	public ResponseEntity<ApiResponse<NotificationLogResponse>> sendTestEmail(
			@Valid @RequestBody TestEmailNotificationRequest body,
			HttpServletRequest request) {
		return created(notificationService.sendTestEmail(body), "Test email notification processed", request);
	}

	@PostMapping("/test/sms")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_SEND')")
	@Operation(summary = "Send test SMS notification")
	public ResponseEntity<ApiResponse<NotificationLogResponse>> sendTestSms(
			@Valid @RequestBody TestSmsNotificationRequest body,
			HttpServletRequest request) {
		return created(notificationService.sendTestSms(body), "Test SMS notification processed", request);
	}

	@PostMapping("/test/whatsapp")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_SEND')")
	@Operation(summary = "Send test WhatsApp notification")
	public ResponseEntity<ApiResponse<NotificationLogResponse>> sendTestWhatsApp(
			@Valid @RequestBody TestWhatsAppNotificationRequest body,
			HttpServletRequest request) {
		return created(notificationService.sendTestWhatsApp(body), "Test WhatsApp notification processed", request);
	}

	@GetMapping("/logs")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	@Operation(summary = "List notification logs")
	public ResponseEntity<ApiResponse<PageResponse<NotificationLogResponse>>> logs(
			@RequestParam(required = false) NotificationChannel channel,
			@RequestParam(required = false) NotificationStatus status,
			@RequestParam(required = false) String referenceType,
			@RequestParam(required = false) UUID referenceId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				notificationService.logs(channel, status, referenceType, referenceId, pageRequest),
				"Notification logs fetched successfully",
				request);
	}

	@GetMapping("/templates")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	@Operation(summary = "List notification templates")
	public ResponseEntity<ApiResponse<PageResponse<NotificationTemplateResponse>>> templates(
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(notificationService.templates(pageRequest), "Notification templates fetched successfully", request);
	}

	@PostMapping("/templates")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	@Operation(summary = "Create notification template")
	public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
			@Valid @RequestBody NotificationTemplateRequest body,
			HttpServletRequest request) {
		return created(notificationService.createTemplate(body), "Notification template created successfully", request);
	}

	@GetMapping("/templates/{templateId}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_READ')")
	@Operation(summary = "Get notification template")
	public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getTemplate(
			@PathVariable UUID templateId,
			HttpServletRequest request) {
		return ok(notificationService.getTemplate(templateId), "Notification template fetched successfully", request);
	}

	@PutMapping("/templates/{templateId}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	@Operation(summary = "Update notification template")
	public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
			@PathVariable UUID templateId,
			@Valid @RequestBody NotificationTemplateRequest body,
			HttpServletRequest request) {
		return ok(notificationService.updateTemplate(templateId, body), "Notification template updated successfully", request);
	}

	@DeleteMapping("/templates/{templateId}")
	@PreAuthorize("hasAuthority('NOTIFICATIONS_MANAGE')")
	@Operation(summary = "Delete notification template")
	public ResponseEntity<ApiResponse<Void>> deleteTemplate(
			@PathVariable UUID templateId,
			HttpServletRequest request) {
		notificationService.deleteTemplate(templateId);
		return ok(null, "Notification template deleted successfully", request);
	}

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
