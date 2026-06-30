package com.school.erp.modules.teachers.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;
import com.school.erp.modules.teachers.domain.TeacherAssignmentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Teacher academic assignment payload.")
public record TeacherAssignmentRequest(
		@NotNull UUID academicYearId,
		@NotNull TeacherAssignmentType assignmentType,
		UUID classId,
		UUID sectionId,
		UUID subjectId,
		LocalDate effectiveFrom,
		TeacherAssignmentStatus status) {
}
