package com.school.erp.modules.users.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.users.api.dto.AdminResetPasswordRequest;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.UserCreateRequest;
import com.school.erp.modules.users.api.dto.UserResponse;
import com.school.erp.modules.users.api.dto.UserRoleUpdateRequest;
import com.school.erp.modules.users.api.dto.UserSearchRequest;
import com.school.erp.modules.users.api.dto.UserUpdateRequest;
import com.school.erp.modules.users.application.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User accounts, roles, status, and admin password resets.")
public class UserController {

	private final UserService userService;

	@PostMapping
	@PreAuthorize("hasAuthority('USERS_CREATE')")
	@Operation(summary = "Create user")
	public ResponseEntity<ApiResponse<UserResponse>> create(
			@Valid @RequestBody UserCreateRequest request,
			HttpServletRequest httpRequest) {
		return created(userService.create(request), "User created successfully", httpRequest);
	}

	@PutMapping("/{userId}")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Update user")
	public ResponseEntity<ApiResponse<UserResponse>> update(
			@PathVariable UUID userId,
			@Valid @RequestBody UserUpdateRequest request,
			HttpServletRequest httpRequest) {
		return ok(userService.update(userId, request), "User updated successfully", httpRequest);
	}

	@GetMapping("/{userId}")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "Get user by ID")
	public ResponseEntity<ApiResponse<UserResponse>> get(
			@PathVariable UUID userId,
			HttpServletRequest httpRequest) {
		return ok(userService.get(userId), "User fetched successfully", httpRequest);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "Search users")
	public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> search(
			@Valid @ParameterObject UserSearchRequest searchRequest,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest httpRequest) {
		return ok(userService.search(searchRequest, pageRequest), "Users fetched successfully", httpRequest);
	}

	@GetMapping("/roles")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "List roles")
	public ResponseEntity<ApiResponse<List<RoleResponse>>> roles(HttpServletRequest httpRequest) {
		return ok(userService.listRoles(), "Roles fetched successfully", httpRequest);
	}

	@PatchMapping("/{userId}/activate")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Activate user")
	public ResponseEntity<ApiResponse<UserResponse>> activate(
			@PathVariable UUID userId,
			HttpServletRequest httpRequest) {
		return ok(userService.activate(userId), "User activated successfully", httpRequest);
	}

	@PatchMapping("/{userId}/deactivate")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Deactivate user")
	public ResponseEntity<ApiResponse<UserResponse>> deactivate(
			@PathVariable UUID userId,
			HttpServletRequest httpRequest) {
		return ok(userService.deactivate(userId), "User deactivated successfully", httpRequest);
	}

	@PostMapping("/{userId}/roles")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Assign role")
	public ResponseEntity<ApiResponse<UserResponse>> assignRole(
			@PathVariable UUID userId,
			@Valid @RequestBody UserRoleUpdateRequest request,
			HttpServletRequest httpRequest) {
		return ok(userService.assignRole(userId, request.role()), "Role assigned successfully", httpRequest);
	}

	@DeleteMapping("/{userId}/roles")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Remove role")
	public ResponseEntity<ApiResponse<UserResponse>> removeRole(
			@PathVariable UUID userId,
			@Valid @RequestBody UserRoleUpdateRequest request,
			HttpServletRequest httpRequest) {
		return ok(userService.removeRole(userId, request.role()), "Role removed successfully", httpRequest);
	}

	@PostMapping("/{userId}/reset-password")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Reset user password by admin")
	public ResponseEntity<ApiResponse<Void>> resetPassword(
			@PathVariable UUID userId,
			@Valid @RequestBody AdminResetPasswordRequest request,
			HttpServletRequest httpRequest) {
		userService.resetPassword(userId, request);
		return ok(null, "Password reset successfully", httpRequest);
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasAuthority('USERS_DELETE')")
	@Operation(summary = "Soft delete user")
	public ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable UUID userId,
			HttpServletRequest httpRequest) {
		userService.delete(userId);
		return ok(null, "User deleted successfully", httpRequest);
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
