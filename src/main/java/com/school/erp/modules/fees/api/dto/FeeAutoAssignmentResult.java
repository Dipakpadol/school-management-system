package com.school.erp.modules.fees.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Summary of automatic student fee assignment.")
public record FeeAutoAssignmentResult(
		UUID studentId,
		int classFeesAssignedCount,
		int hostelFeesAssignedCount,
		int transportFeesAssignedCount,
		List<String> warnings,
		int skippedDuplicates) {

	public FeeAutoAssignmentResult {
		warnings = warnings == null ? List.of() : List.copyOf(warnings);
	}

	public static FeeAutoAssignmentResult empty(UUID studentId) {
		return new FeeAutoAssignmentResult(studentId, 0, 0, 0, List.of(), 0);
	}

	public FeeAutoAssignmentResult merge(FeeAutoAssignmentResult other) {
		if (other == null) {
			return this;
		}
		java.util.ArrayList<String> mergedWarnings = new java.util.ArrayList<>(warnings);
		mergedWarnings.addAll(other.warnings());
		return new FeeAutoAssignmentResult(
				studentId,
				classFeesAssignedCount + other.classFeesAssignedCount(),
				hostelFeesAssignedCount + other.hostelFeesAssignedCount(),
				transportFeesAssignedCount + other.transportFeesAssignedCount(),
				mergedWarnings,
				skippedDuplicates + other.skippedDuplicates());
	}
}
