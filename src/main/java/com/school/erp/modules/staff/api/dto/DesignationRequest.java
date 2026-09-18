package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record DesignationRequest(
		@NotBlank String name,
		UUID departmentId,
		String description,
		Boolean active) {
}
