package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Fee structure installment template.")
public record FeeStructureInstallmentRequest(
		@Min(1) @Schema(example = "1") int sequenceNo,
		@NotBlank @Size(max = 120) @Schema(example = "First Term") String title,
		@NotNull @Schema(example = "2026-06-15") LocalDate dueDate,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "15000.00") BigDecimal amount) {
}
