package com.school.erp.modules.students.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Class and section assignment details.")
public record ClassSectionAssignmentResponse(
		UUID id,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		String academicYear,
		String className,
		String sectionName,
		String rollNumber,
		LocalDate effectiveFrom,
		LocalDate effectiveTo,
		boolean active,
		Instant createdAt) {
}
