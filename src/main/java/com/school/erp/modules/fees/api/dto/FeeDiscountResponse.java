package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.fees.domain.DiscountCalculationType;
import com.school.erp.modules.fees.domain.DiscountType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee discount response.")
public record FeeDiscountResponse(
		UUID id,
		UUID installmentId,
		DiscountType discountType,
		DiscountCalculationType calculationType,
		BigDecimal value,
		BigDecimal amount,
		String reason,
		String approvedBy,
		Instant approvedAt) {
}
