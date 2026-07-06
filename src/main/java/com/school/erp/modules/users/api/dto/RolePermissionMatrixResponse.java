package com.school.erp.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import com.school.erp.modules.users.domain.RoleStatus;

public record RolePermissionMatrixResponse(
		UUID roleId,
		String roleName,
		String displayName,
		String description,
		RoleStatus status,
		boolean systemRole,
		List<PermissionResponse> permissions) {
}
