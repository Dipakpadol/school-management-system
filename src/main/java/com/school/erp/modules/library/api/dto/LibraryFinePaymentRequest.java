package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record LibraryFinePaymentRequest(
		@DecimalMin("0.00") BigDecimal amount,
		@Size(max = 500) String note) {
}
