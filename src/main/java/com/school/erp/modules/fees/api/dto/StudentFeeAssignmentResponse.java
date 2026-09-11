package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeScope;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student fee assignment response.")
public record StudentFeeAssignmentResponse(
		UUID id,
		UUID assignmentId,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID feeStructureId,
		String feeStructureName,
		String feeCategoryName,
		UUID academicYearId,
		String academicYearName,
		UUID classId,
		UUID sectionId,
		FeeScope feeScope,
		FeeScope sourceType,
		UUID sourceReferenceId,
		UUID hostelId,
		String hostelName,
		UUID hostelRoomId,
		String hostelRoomNumber,
		String roomType,
		UUID transportRouteId,
		String transportRouteName,
		UUID transportPickupPointId,
		String transportPickupPointName,
		String academicYear,
		String className,
		String sectionName,
		LocalDate assignedDate,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal lateFeeAmount,
		BigDecimal payableAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount,
		BigDecimal amount,
		BigDecimal pendingAmount,
		FeeAssignmentStatus status,
		String notes,
		List<StudentFeeInstallmentResponse> installments,
		List<FeeDiscountResponse> discounts,
		List<FeePaymentResponse> payments,
		Instant createdAt,
		Instant updatedAt) {
}
