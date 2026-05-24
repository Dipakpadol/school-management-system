package com.school.erp.modules.fees.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Student fee assignment request.")
public record StudentFeeAssignmentRequest(
		@NotNull @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID studentId,
		@NotNull @Schema(example = "803443ff-4c45-47b5-8d70-9782f8fbce44") UUID feeStructureId,
		@NotNull @Schema(example = "2026-04-01") LocalDate assignedDate,
		@Size(max = 500) @Schema(example = "Assigned during admission.") String notes) {
}
