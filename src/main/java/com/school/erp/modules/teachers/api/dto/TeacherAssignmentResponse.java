package com.school.erp.modules.teachers.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;
import com.school.erp.modules.teachers.domain.TeacherAssignmentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher academic assignment response.")
public record TeacherAssignmentResponse(
		UUID id,
		UUID teacherId,
		String teacherName,
		UUID academicYearId,
		String academicYear,
		TeacherAssignmentType assignmentType,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		UUID subjectId,
		String subjectName,
		TeacherAssignmentStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
