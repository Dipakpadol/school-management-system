package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class in an academic year.")
public record ClassResponse(
		UUID id,
		UUID academicYearId,
		String code,
		String name,
		int displayOrder,
		boolean active) {
}
