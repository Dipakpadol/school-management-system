package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Subject assigned to a division.")
public record DivisionSubjectResponse(
		UUID id,
		UUID divisionId,
		UUID subjectId,
		String subjectCode,
		String subjectName,
		TeacherSummaryResponse teacher,
		boolean active) {
}
