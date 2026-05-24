package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student fee assignment response.")
public record StudentFeeAssignmentResponse(
		UUID id,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID feeStructureId,
		String feeStructureName,
		String academicYear,
		String className,
		String sectionName,
		LocalDate assignedDate,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal lateFeeAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount,
		FeeAssignmentStatus status,
		String notes,
		List<StudentFeeInstallmentResponse> installments,
		List<FeeDiscountResponse> discounts,
		List<FeePaymentResponse> payments,
		Instant createdAt,
		Instant updatedAt) {
}
