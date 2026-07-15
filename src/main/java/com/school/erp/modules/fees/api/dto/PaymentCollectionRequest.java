package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.PaymentMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Payment collection request.")
public record PaymentCollectionRequest(
		@NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "5000.00") BigDecimal amount,
		@NotNull @Schema(example = "2026-06-20") LocalDate paymentDate,
		@NotNull @Schema(example = "UPI") PaymentMode paymentMode,
		@Size(max = 120) @Schema(example = "UPI-243424234") String referenceNumber,
		@NotBlank @Size(max = 160) @Schema(example = "Rajesh Sharma") String payerName,
		@Size(max = 100) @Schema(example = "accountant@school.test") String collectedBy,
		@Size(max = 500) @Schema(example = "First installment payment.") String remarks,
		@Schema(description = "When true, active late fee rules are assessed before allocation.", example = "true")
		boolean assessLateFee,
		@Schema(description = "Academic year context for student-level payment collection.") UUID academicYearId,
		@Schema(description = "Specific student fee assignment to collect against.") UUID assignmentId) {

	public PaymentCollectionRequest(
			BigDecimal amount,
			LocalDate paymentDate,
			PaymentMode paymentMode,
			String referenceNumber,
			String payerName,
			String collectedBy,
			String remarks,
			boolean assessLateFee) {
		this(
				amount,
				paymentDate,
				paymentMode,
				referenceNumber,
				payerName,
				collectedBy,
				remarks,
				assessLateFee,
				null,
				null);
	}

	public PaymentCollectionRequest(
			BigDecimal amount,
			LocalDate paymentDate,
			PaymentMode paymentMode,
			String referenceNumber,
			String payerName,
			String collectedBy,
			String remarks,
			boolean assessLateFee,
			UUID assignmentId) {
		this(
				amount,
				paymentDate,
				paymentMode,
				referenceNumber,
				payerName,
				collectedBy,
				remarks,
				assessLateFee,
				null,
				assignmentId);
	}
}
