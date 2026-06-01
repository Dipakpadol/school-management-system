package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StudentResultResponse(
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		BigDecimal totalMarks,
		BigDecimal maxMarks,
		BigDecimal percentage,
		String grade,
		boolean passed,
		Integer rank,
		List<SubjectResultResponse> subjects) {
}
