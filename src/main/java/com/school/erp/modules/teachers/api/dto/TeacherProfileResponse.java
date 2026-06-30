package com.school.erp.modules.teachers.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher profile response with profile-tab data.")
public record TeacherProfileResponse(
		TeacherResponse personalDetails,
		List<TeacherAssignmentResponse> academicAssignments,
		List<TeacherAcademicMappingResponse> classTeacherMappings,
		List<TeacherAcademicMappingResponse> subjectTeacherMappings,
		List<TeacherDocumentResponse> documents,
		String attendanceSummary,
		String payrollSummary,
		String notificationSummary) {
}
