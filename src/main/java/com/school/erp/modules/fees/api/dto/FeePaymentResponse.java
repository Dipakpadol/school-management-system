package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeePaymentStatus;
import com.school.erp.modules.fees.domain.PaymentMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee payment response.")
public record FeePaymentResponse(
		UUID id,
		String receiptNumber,
		BigDecimal amount,
		LocalDate paymentDate,
		PaymentMode paymentMode,
		String referenceNumber,
		String payerName,
		String collectedBy,
		String remarks,
		FeePaymentStatus status,
		List<FeePaymentAllocationResponse> allocations) {
}
