package com.school.erp.modules.academic.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Subject with assigned teachers for a class section.")
public record SubjectTeacherResponse(
		UUID subjectId,
		String subjectCode,
		String subjectName,
		List<TeacherSummaryResponse> teachers) {
}
