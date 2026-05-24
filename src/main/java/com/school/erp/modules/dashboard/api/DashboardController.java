package com.school.erp.modules.dashboard.api;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.dashboard.api.dto.DashboardSummaryResponse;
import com.school.erp.modules.dashboard.application.DashboardService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Dashboard", description = "School ERP operational dashboard APIs.")
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardService dashboardService;

	@Operation(summary = "Get live dashboard summary")
	@GetMapping("/summary")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				dashboardService.summary(),
				"Dashboard summary loaded",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
