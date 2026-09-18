package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

public record DesignationResponse(
		UUID id,
		String name,
		UUID departmentId,
		String departmentName,
		String description,
		boolean active) {
}
