package com.school.erp.modules.academic.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Subject teacher assignment request.")
public record AssignSubjectTeacherRequest(
		@NotNull UUID teacherId,
		@NotNull @Schema(example = "2026-04-01") LocalDate effectiveFrom) {
}
