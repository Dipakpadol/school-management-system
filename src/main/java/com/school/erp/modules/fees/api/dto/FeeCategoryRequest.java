package com.school.erp.modules.fees.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Fee category request.")
public record FeeCategoryRequest(
		@NotBlank @Size(max = 60) @Schema(example = "TUITION") String code,
		@NotBlank @Size(max = 120) @Schema(example = "Tuition Fee") String name,
		@Size(max = 500) @Schema(example = "Regular tuition fee.") String description,
		@Schema(example = "true") boolean active,
		@Min(0) @Schema(example = "1") int sortOrder) {
}
