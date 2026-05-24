package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeReceiptStatus;
import com.school.erp.modules.fees.domain.PaymentMode;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee receipt response.")
public record FeeReceiptResponse(
		UUID id,
		String receiptNumber,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID assignmentId,
		Instant receiptDate,
		BigDecimal totalAmount,
		String payerName,
		PaymentMode paymentMode,
		FeeReceiptStatus status) {
}
