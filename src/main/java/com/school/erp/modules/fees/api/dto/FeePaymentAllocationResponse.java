package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment allocation response.")
public record FeePaymentAllocationResponse(
		UUID installmentId,
		String installmentTitle,
		BigDecimal amount) {
}
