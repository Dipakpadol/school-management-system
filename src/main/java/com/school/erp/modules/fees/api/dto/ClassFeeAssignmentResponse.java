package com.school.erp.modules.fees.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class fee assignment generation summary.")
public record ClassFeeAssignmentResponse(
		UUID academicYearId,
		UUID classId,
		UUID feeStructureId,
		List<UUID> assignedFeeStructures,
		int totalStudents,
		int assignedStudents,
		int skippedStudents,
		int createdAssignments,
		int skippedAssignments,
		List<StudentFeeAssignmentResponse> assignments,
		int feeStructureCount,
		int newAssignmentsCreated,
		int duplicateAssignmentsSkipped,
		List<String> warnings,
		String message) {
}
