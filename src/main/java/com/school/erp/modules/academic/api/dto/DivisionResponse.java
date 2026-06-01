package com.school.erp.modules.academic.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Division details with student count, class teacher, and subjects.")
public record DivisionResponse(
		UUID id,
		UUID classId,
		String code,
		String name,
		Integer capacity,
		int displayOrder,
		boolean active,
		long totalStudents,
		TeacherSummaryResponse classTeacher,
		List<DivisionSubjectResponse> subjects) {
}
