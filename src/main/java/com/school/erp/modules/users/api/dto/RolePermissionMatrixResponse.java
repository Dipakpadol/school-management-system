package com.school.erp.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import com.school.erp.modules.users.domain.RoleName;

public record RolePermissionMatrixResponse(
		UUID roleId,
		RoleName roleName,
		String displayName,
		String description,
		List<PermissionResponse> permissions) {
}
