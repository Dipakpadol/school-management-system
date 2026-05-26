package com.school.erp.common.menu.api.dto;

import java.util.UUID;

public record MenuItemResponse(
		UUID id,
		String moduleId,
		String title,
		String routePath,
		String requiredPermission,
		String iconKey,
		int displayOrder) {
}
