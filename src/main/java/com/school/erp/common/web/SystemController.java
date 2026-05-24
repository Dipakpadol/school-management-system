package com.school.erp.common.web;

import java.time.Instant;

import com.school.erp.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/system")
public class SystemController {

	@GetMapping("/status")
	public ResponseEntity<ApiResponse<SystemStatusResponse>> status(HttpServletRequest request) {
		var status = new SystemStatusResponse("school-erp-api", "UP", Instant.now());
		return ResponseEntity.ok(ApiResponse.success(
				status,
				"School ERP API is running",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}

	public record SystemStatusResponse(String service, String status, Instant timestamp) {
	}
}
