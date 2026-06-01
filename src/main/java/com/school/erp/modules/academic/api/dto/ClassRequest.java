package com.school.erp.modules.academic.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Class create/update payload.")
public record ClassRequest(
		@Size(max = 40) @Schema(example = "CLASS-6") String code,
		@NotBlank @Size(max = 120) @Schema(example = "Class 6") String name,
		@Min(0) @Schema(example = "6") int displayOrder,
		@Schema(example = "true") boolean active) {
}
