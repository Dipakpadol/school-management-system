package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Division create/update payload.")
public record DivisionRequest(
		@Size(max = 40) @Schema(example = "A") String code,
		@NotBlank @Size(max = 120) @Schema(example = "Division A") String name,
		@Min(1) @Schema(example = "40") Integer capacity,
		@Min(0) @Schema(example = "1") int displayOrder,
		@Schema(example = "true") boolean active,
		@Schema(description = "Optional class teacher for this division.") UUID classTeacherId) {
}
