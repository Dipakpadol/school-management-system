package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student fee status within a class.")
public record ClassStudentFeeResponse(
		UUID studentId,
		String admissionNumber,
		String studentName,
		String rollNumber,
		UUID assignmentId,
		UUID feeStructureId,
		String feeStructureName,
		BigDecimal grossAmount,
		BigDecimal discountAmount,
		BigDecimal paidAmount,
		BigDecimal balanceAmount,
		FeeAssignmentStatus status) {
}
