package com.school.erp.modules.fees.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.ClassFeeAssignmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class fee assignment detail.")
public record ClassFeeAssignmentDetailResponse(
		UUID id,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID feeStructureId,
		String feeStructureName,
		LocalDate assignedDate,
		ClassFeeAssignmentStatus status,
		String assignedBy) {
}
