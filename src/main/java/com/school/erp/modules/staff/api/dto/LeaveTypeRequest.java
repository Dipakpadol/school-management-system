package com.school.erp.modules.staff.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LeaveTypeRequest(
		@NotBlank String name,
		String description,
		Boolean paid,
		Boolean active) {
}
