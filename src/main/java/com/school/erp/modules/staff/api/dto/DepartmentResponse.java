package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

public record DepartmentResponse(
		UUID id,
		String name,
		String description,
		boolean active) {
}
