package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student fee summary across assigned fee structures.")
public record StudentFeeSummaryResponse(
		UUID studentId,
		String admissionNumber,
		String studentName,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal lateFeeAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount,
		List<StudentFeeAssignmentResponse> assignments) {
}
