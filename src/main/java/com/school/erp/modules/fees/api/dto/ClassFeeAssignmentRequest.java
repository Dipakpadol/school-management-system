package com.school.erp.modules.fees.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Assign an active class fee structure to students in a class.")
public record ClassFeeAssignmentRequest(
		@NotNull @Schema(description = "Fee structure UUID") UUID feeStructureId,
		@NotNull @Schema(example = "2026-04-01") LocalDate assignedDate,
		@Size(max = 500) @Schema(example = "Generated from class fee assignment") String notes,
		@Schema(description = "When true, skips students who already have this fee structure.", example = "true")
		boolean skipExisting) {
}
