package com.school.erp.modules.exams.api.dto;

import java.util.List;
import java.util.UUID;

public record StudentExamResultsResponse(
		UUID studentId,
		String studentName,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		List<StudentExamResultDetailResponse> results) {
}
