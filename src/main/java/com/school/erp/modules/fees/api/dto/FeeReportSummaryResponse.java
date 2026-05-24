package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee report summary response.")
public record FeeReportSummaryResponse(
		long assignments,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal lateFeeAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount) {
}
