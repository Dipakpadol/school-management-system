package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Division subject assignment payload.")
public record DivisionSubjectRequest(
		@Schema(description = "Existing subject id. If omitted, subjectCode and subjectName are used.") UUID subjectId,
		@Size(max = 40) @Schema(example = "MATH") String subjectCode,
		@Size(max = 120) @Schema(example = "Mathematics") String subjectName,
		@Size(max = 500) String description,
		@Schema(description = "Optional assigned subject teacher.") UUID teacherId,
		@Schema(example = "true") Boolean active) {
}
