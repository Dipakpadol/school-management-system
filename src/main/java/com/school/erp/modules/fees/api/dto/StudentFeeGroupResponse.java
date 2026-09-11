package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.school.erp.modules.fees.domain.FeeScope;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Grouped student fee totals by fee source.")
public record StudentFeeGroupResponse(
		FeeScope sourceType,
		String label,
	BigDecimal grossAmount,
	BigDecimal discountAmount,
	BigDecimal lateFeeAmount,
	BigDecimal payableAmount,
	BigDecimal paidAmount,
	BigDecimal balanceAmount,
		List<StudentFeeAssignmentResponse> assignments) {
}
