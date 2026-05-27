package com.school.erp.modules.academic.api.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher mappings visible at class and division level.")
public record ClassSectionTeachersResponse(
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		TeacherSummaryResponse classTeacher,
		List<SubjectTeacherResponse> subjectTeachers) {
}
