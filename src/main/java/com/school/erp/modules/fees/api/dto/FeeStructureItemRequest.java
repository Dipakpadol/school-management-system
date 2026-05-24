package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Fee structure charge line.")
public record FeeStructureItemRequest(
		@NotNull @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID categoryId,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "25000.00") BigDecimal amount,
		@Schema(example = "true") boolean mandatory,
		@Min(0) @Schema(example = "1") int sortOrder) {
}
