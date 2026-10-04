package com.school.erp.modules.portal.api.dto;

import java.util.UUID;

import com.school.erp.modules.teachers.domain.TeacherAssignmentType;

public record PortalTeacherScopeResponse(
		String source,
		TeacherAssignmentType assignmentType,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		UUID subjectId,
		String subjectName) {
}
