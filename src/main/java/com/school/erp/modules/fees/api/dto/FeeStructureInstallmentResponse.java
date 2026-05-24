package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee structure installment response.")
public record FeeStructureInstallmentResponse(
		UUID id,
		int sequenceNo,
		String title,
		LocalDate dueDate,
		BigDecimal amount) {
}
