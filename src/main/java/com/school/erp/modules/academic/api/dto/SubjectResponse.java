package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Academic subject.")
public record SubjectResponse(
		UUID id,
		String code,
		String name,
		String description,
		boolean active) {
}
