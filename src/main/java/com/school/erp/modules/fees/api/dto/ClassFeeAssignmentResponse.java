package com.school.erp.modules.fees.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class fee assignment generation summary.")
public record ClassFeeAssignmentResponse(
		UUID classId,
		UUID feeStructureId,
		int totalStudents,
		int createdAssignments,
		int skippedAssignments,
		List<StudentFeeAssignmentResponse> assignments) {
}
