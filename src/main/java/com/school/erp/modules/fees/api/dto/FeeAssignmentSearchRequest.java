package com.school.erp.modules.fees.api.dto;

import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Student fee assignment search filters.")
public record FeeAssignmentSearchRequest(
		UUID studentId,
		@Size(max = 40) @Schema(example = "ADM-2026-0001") String admissionNumber,
		@Size(max = 120) @Schema(example = "aarav") String studentName,
		@Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@Schema(example = "OVERDUE") FeeAssignmentStatus status) {
}
