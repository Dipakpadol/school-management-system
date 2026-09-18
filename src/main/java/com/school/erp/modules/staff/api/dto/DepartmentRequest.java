package com.school.erp.modules.staff.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
		@NotBlank String name,
		String description,
		Boolean active) {
}
