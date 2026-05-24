package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;

import com.school.erp.modules.fees.domain.LateFeeCalculationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Late fee rule request.")
public record LateFeeRuleRequest(
		@NotBlank @Size(max = 120) @Schema(example = "Standard late fee") String name,
		@NotBlank @Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@Min(0) @Schema(example = "5") int graceDays,
		@NotNull @Schema(example = "PER_DAY") LateFeeCalculationType calculationType,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "25.00") BigDecimal amount,
		@DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "1000.00") BigDecimal maxAmount,
		@Schema(example = "true") boolean active) {
}
