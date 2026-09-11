package com.school.erp.modules.users.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.users.api.dto.PermissionResponse;
import com.school.erp.modules.users.api.dto.PermissionRequest;
import com.school.erp.modules.users.api.dto.PermissionSearchRequest;
import com.school.erp.modules.users.api.dto.RolePermissionMatrixResponse;
import com.school.erp.modules.users.api.dto.RoleRequest;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.RoleSearchRequest;
import com.school.erp.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.school.erp.modules.users.application.RoleService;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Role & Permission Management", description = "Role CRUD and permission assignment APIs.")
public class RoleController {

	private final RoleService roleService;

	@PostMapping("/v1/roles")
	@PreAuthorize("hasAuthority('USERS_CREATE')")
	@Operation(summary = "Create role")
	public ResponseEntity<ApiResponse<RoleResponse>> create(
			@Valid @RequestBody RoleRequest request,
			HttpServletRequest httpRequest) {
		return created(roleService.create(request), "Role created successfully", httpRequest);
	}

	@PutMapping("/v1/roles/{roleId}")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Update role")
	public ResponseEntity<ApiResponse<RoleResponse>> update(
			@PathVariable UUID roleId,
			@Valid @RequestBody RoleRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.update(roleId, request), "Role updated successfully", httpRequest);
	}

	@GetMapping("/v1/roles/{roleId}")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "Get role by ID")
	public ResponseEntity<ApiResponse<RoleResponse>> get(
			@PathVariable UUID roleId,
			HttpServletRequest httpRequest) {
		return ok(roleService.get(roleId), "Role fetched successfully", httpRequest);
	}

	@GetMapping("/v1/roles")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "List roles")
	public ResponseEntity<ApiResponse<List<RoleResponse>>> list(
			@Valid @ParameterObject RoleSearchRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.list(request), "Roles fetched successfully", httpRequest);
	}

	@DeleteMapping("/v1/roles/{roleId}")
	@PreAuthorize("hasAuthority('USERS_DELETE')")
	@Operation(summary = "Soft delete role")
	public ResponseEntity<ApiResponse<Void>> delete(
			@PathVariable UUID roleId,
			HttpServletRequest httpRequest) {
		roleService.delete(roleId);
		return ok(null, "Role deleted successfully", httpRequest);
	}

	@GetMapping("/v1/permissions")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "List permissions")
	public ResponseEntity<ApiResponse<List<PermissionResponse>>> permissions(
			@Valid @ParameterObject PermissionSearchRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.permissions(request), "Permissions fetched successfully", httpRequest);
	}

	@PostMapping("/v1/permissions")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Create permission")
	public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
			@Valid @RequestBody PermissionRequest request,
			HttpServletRequest httpRequest) {
		return created(roleService.createPermission(request), "Permission created successfully", httpRequest);
	}

	@PutMapping("/v1/permissions/{permissionId}")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Update permission")
	public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
			@PathVariable UUID permissionId,
			@Valid @RequestBody PermissionRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.updatePermission(permissionId, request), "Permission updated successfully", httpRequest);
	}

	@GetMapping("/v1/permissions/{permissionId}")
	@PreAuthorize("hasAuthority('USERS_READ')")
	@Operation(summary = "Get permission")
	public ResponseEntity<ApiResponse<PermissionResponse>> getPermission(
			@PathVariable UUID permissionId,
			HttpServletRequest httpRequest) {
		return ok(roleService.getPermission(permissionId), "Permission fetched successfully", httpRequest);
	}

	@DeleteMapping("/v1/permissions/{permissionId}")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Soft delete permission")
	public ResponseEntity<ApiResponse<Void>> deletePermission(
			@PathVariable UUID permissionId,
			HttpServletRequest httpRequest) {
		roleService.deletePermission(permissionId);
		return ok(null, "Permission deleted successfully", httpRequest);
	}

	@PostMapping("/v1/roles/{roleId}/permissions")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Assign role permissions")
	public ResponseEntity<ApiResponse<RolePermissionMatrixResponse>> assignPermissions(
			@PathVariable UUID roleId,
			@Valid @RequestBody UpdateRolePermissionsRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.updateRolePermissions(roleId, request), "Role permissions assigned successfully", httpRequest);
	}

	@PutMapping("/v1/roles/{roleId}/permissions")
	@PreAuthorize("hasAuthority('USERS_UPDATE')")
	@Operation(summary = "Update role permissions")
	public ResponseEntity<ApiResponse<RolePermissionMatrixResponse>> updatePermissions(
			@PathVariable UUID roleId,
			@Valid @RequestBody UpdateRolePermissionsRequest request,
			HttpServletRequest httpRequest) {
		return ok(roleService.updateRolePermissions(roleId, request), "Role permissions updated successfully", httpRequest);
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
