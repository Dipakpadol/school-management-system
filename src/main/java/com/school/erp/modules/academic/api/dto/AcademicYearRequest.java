package com.school.erp.modules.academic.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Academic year create/update payload.")
public record AcademicYearRequest(
		@Size(max = 40) @Schema(example = "AY-2026-27") String code,
		@NotBlank @Size(max = 120) @Schema(example = "2026-2027") String name,
		@NotNull @Schema(example = "2026-04-01") LocalDate startDate,
		@NotNull @Schema(example = "2027-03-31") LocalDate endDate,
		@Schema(example = "true") boolean active,
		@Size(max = 500) @Schema(example = "Regular academic session.") String description) {
}
