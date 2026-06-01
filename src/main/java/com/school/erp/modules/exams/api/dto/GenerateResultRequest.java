package com.school.erp.modules.exams.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record GenerateResultRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID classId,
		@NotNull UUID sectionId,
		UUID examTypeId) {
}
