package com.school.erp.modules.academic.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Academic year available for student hierarchy navigation.")
public record AcademicYearResponse(
		UUID id,
		String code,
		String name,
		LocalDate startDate,
		LocalDate endDate,
		boolean active) {
}
