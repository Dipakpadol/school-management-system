package com.school.erp.modules.users.api.dto;

import java.util.UUID;

import com.school.erp.modules.users.domain.RoleName;

public record RoleResponse(
		UUID id,
		RoleName name,
		String displayName,
		String description) {
}
