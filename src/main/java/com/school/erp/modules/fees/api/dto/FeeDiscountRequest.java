package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.school.erp.modules.fees.domain.DiscountCalculationType;
import com.school.erp.modules.fees.domain.DiscountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Discount request. If installmentId is omitted, the discount is distributed across outstanding installments.")
public record FeeDiscountRequest(
		@Schema(example = "d2c89d70-68e4-47cf-a07b-f8792ef3378a") UUID installmentId,
		@NotNull @Schema(example = "SCHOLARSHIP") DiscountType discountType,
		@NotNull @Schema(example = "FLAT") DiscountCalculationType calculationType,
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "1000.00") BigDecimal value,
		@NotBlank @Size(max = 500) @Schema(example = "Merit scholarship approved by principal.") String reason,
		@Size(max = 100) @Schema(example = "principal@school.test") String approvedBy) {
}
