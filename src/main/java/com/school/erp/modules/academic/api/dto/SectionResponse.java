package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Section or division in a class.")
public record SectionResponse(
		UUID id,
		UUID classId,
		String code,
		String name,
		Integer capacity,
		int displayOrder,
		boolean active) {
}
