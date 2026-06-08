package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentExamResultDetailResponse(
		UUID resultId,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		UUID examTypeId,
		String examType,
		String examName,
		UUID examScheduleId,
		List<StudentSubjectExamResultResponse> subjectResults,
		BigDecimal totalMarks,
		BigDecimal obtainedMarks,
		BigDecimal percentage,
		String grade,
		String passFailStatus,
		Integer rank,
		Instant resultDate) {
}
