package com.school.erp.modules.students.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.students.domain.StudentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student search result summary.")
public record StudentSummaryResponse(
		UUID id,
		String admissionNumber,
		String displayName,
		StudentStatus status,
		LocalDate admissionDate,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		String className,
		String sectionName,
		String rollNumber) {
}
