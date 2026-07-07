package com.school.erp.modules.users.api.dto;

import java.util.UUID;

public record PermissionResponse(
		UUID id,
		String code,
		String name,
		String moduleName,
		String description,
		String status,
		boolean assigned) {
}
