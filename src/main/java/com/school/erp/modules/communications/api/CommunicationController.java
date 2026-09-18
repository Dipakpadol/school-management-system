package com.school.erp.modules.communications.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.communications.api.dto.CommunicationRequest;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.communications.application.CommunicationService;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/communications")
@RequiredArgsConstructor
@Tag(name = "Communication", description = "Announcements, circulars, notices, and events.")
public class CommunicationController {

	private final CommunicationService communicationService;

	@GetMapping
	@PreAuthorize("hasAuthority('COMMUNICATION_READ')")
	public ResponseEntity<ApiResponse<PageResponse<CommunicationResponse>>> communications(
			@RequestParam(required = false) CommunicationType type,
			@RequestParam(required = false) CommunicationStatus status,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				communicationService.communications(type, status, pageRequest),
				"Communications fetched successfully",
				request);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('COMMUNICATION_CREATE')")
	public ResponseEntity<ApiResponse<CommunicationResponse>> create(
			@Valid @RequestBody CommunicationRequest body,
			HttpServletRequest request) {
		return created(communicationService.create(body), "Communication created successfully", request);
	}

	@PutMapping("/{communicationId}")
	@PreAuthorize("hasAuthority('COMMUNICATION_UPDATE')")
	public ResponseEntity<ApiResponse<CommunicationResponse>> update(
			@PathVariable UUID communicationId,
			@Valid @RequestBody CommunicationRequest body,
			HttpServletRequest request) {
		return ok(communicationService.update(communicationId, body), "Communication updated successfully", request);
	}

	@PatchMapping("/{communicationId}/publish")
	@PreAuthorize("hasAuthority('COMMUNICATION_PUBLISH')")
	public ResponseEntity<ApiResponse<CommunicationResponse>> publish(
			@PathVariable UUID communicationId,
			HttpServletRequest request) {
		return ok(communicationService.publish(communicationId), "Communication published successfully", request);
	}

	@PatchMapping("/{communicationId}/unpublish")
	@PreAuthorize("hasAuthority('COMMUNICATION_PUBLISH')")
	public ResponseEntity<ApiResponse<CommunicationResponse>> unpublish(
			@PathVariable UUID communicationId,
			HttpServletRequest request) {
		return ok(communicationService.unpublish(communicationId), "Communication unpublished successfully", request);
	}

	@PatchMapping("/{communicationId}/archive")
	@PreAuthorize("hasAuthority('COMMUNICATION_UPDATE')")
	public ResponseEntity<ApiResponse<CommunicationResponse>> archive(
			@PathVariable UUID communicationId,
			HttpServletRequest request) {
		return ok(communicationService.archive(communicationId), "Communication archived successfully", request);
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
