package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MarksEntryResponse(
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		UUID examScheduleId,
		UUID subjectId,
		BigDecimal maxMarks,
		BigDecimal passingMarks,
		int totalRecords,
		List<ExamMarkResponse> records) {
}
