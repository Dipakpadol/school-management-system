package com.school.erp.modules.students.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Class and section assignment for a student.")
public record ClassSectionAssignmentRequest(
		@Schema(description = "Academic year UUID for hierarchy-based assignment.") UUID academicYearId,
		@Schema(description = "Class UUID for hierarchy-based assignment.") UUID classId,
		@Schema(description = "Section UUID for hierarchy-based assignment.") UUID sectionId,
		@NotBlank @Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@NotBlank @Size(max = 80) @Schema(example = "Class 6") String className,
		@NotBlank @Size(max = 80) @Schema(example = "A") String sectionName,
		@Size(max = 30) @Schema(example = "23") String rollNumber,
		@NotNull @Schema(example = "2026-04-01") LocalDate effectiveFrom) {
}
