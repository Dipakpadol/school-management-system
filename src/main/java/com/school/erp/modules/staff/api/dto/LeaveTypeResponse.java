package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

public record LeaveTypeResponse(
		UUID id,
		String name,
		String description,
		boolean paid,
		boolean active) {
}
