package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeInstallmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student fee installment response.")
public record StudentFeeInstallmentResponse(
		UUID id,
		int sequenceNo,
		String title,
		LocalDate dueDate,
		BigDecimal amount,
		BigDecimal discountAmount,
		BigDecimal lateFeeAmount,
		BigDecimal payableAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount,
		FeeInstallmentStatus status) {
}
