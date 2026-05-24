package com.school.erp.modules.fees.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Schema(description = "Fee structure request.")
public record FeeStructureRequest(
		@NotBlank @Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@NotBlank @Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@NotBlank @Size(max = 140) @Schema(example = "Class 6 Annual Fee") String name,
		@Size(max = 500) @Schema(example = "Annual fee structure for Class 6.") String description,
		@Schema(example = "true") boolean activate,
		@Valid @NotEmpty List<FeeStructureItemRequest> items,
		@Valid @NotEmpty List<FeeStructureInstallmentRequest> installments) {
}
