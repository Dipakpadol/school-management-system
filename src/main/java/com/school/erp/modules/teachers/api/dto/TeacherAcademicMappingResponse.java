package com.school.erp.modules.teachers.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.teachers.domain.TeacherAssignmentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher mapping from Academic Management.")
public record TeacherAcademicMappingResponse(
		UUID mappingId,
		String source,
		TeacherAssignmentType assignmentType,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		UUID subjectId,
		String subjectName,
		LocalDate effectiveFrom,
		LocalDate effectiveTo,
		boolean active) {
}
