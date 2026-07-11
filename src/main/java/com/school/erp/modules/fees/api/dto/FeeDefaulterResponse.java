package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeScope;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee defaulter response.")
public record FeeDefaulterResponse(
		UUID assignmentId,
		UUID studentId,
		String admissionNumber,
		String studentName,
		String academicYear,
		String className,
		String sectionName,
		FeeScope sourceType,
		BigDecimal totalAmount,
		BigDecimal paidAmount,
		BigDecimal pendingAmount,
		LocalDate dueDate,
		long overdueDays,
		String contactNumber,
		String parentName,
		BigDecimal balanceAmount,
		LocalDate oldestDueDate,
		long overdueInstallments) {
}
