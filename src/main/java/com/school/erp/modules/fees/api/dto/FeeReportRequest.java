package com.school.erp.modules.fees.api.dto;

import com.school.erp.modules.fees.domain.FeeAssignmentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Fee report filters.")
public record FeeReportRequest(
		@Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@Schema(example = "PARTIALLY_PAID") FeeAssignmentStatus status) {
}
