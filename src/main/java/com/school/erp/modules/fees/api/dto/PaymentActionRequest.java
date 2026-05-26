package com.school.erp.modules.fees.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payment reversal, void, or refund request.")
public record PaymentActionRequest(
		@Size(max = 500) @Schema(example = "Duplicate payment captured during counter collection.") String reason,
		@Schema(example = "2026-05-26") LocalDate actionDate,
		@Size(max = 100) @Schema(example = "accounts.manager") String processedBy) {
}
