package com.school.erp.common.menu.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.menu.api.dto.MenuItemResponse;
import com.school.erp.common.menu.application.MenuService;
import com.school.erp.common.web.CorrelationIdFilter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/menus")
@RequiredArgsConstructor
@Tag(name = "Menus", description = "Role-aware menu items for the admin panel.")
public class MenuController {

	private final MenuService menuService;

	@GetMapping("/current")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Get current user menu")
	public ResponseEntity<ApiResponse<List<MenuItemResponse>>> currentMenu(
			@AuthenticationPrincipal Jwt jwt,
			HttpServletRequest request) {
		List<MenuItemResponse> menu = menuService.currentUserMenu(UUID.fromString(jwt.getSubject()));
		return ResponseEntity.ok(ApiResponse.success(
				menu,
				"Menu fetched successfully",
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}
}
