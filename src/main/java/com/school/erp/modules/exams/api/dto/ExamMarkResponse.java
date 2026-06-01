package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExamMarkResponse(
		UUID id,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		UUID examScheduleId,
		UUID subjectId,
		String subjectName,
		ExamStudentResponse student,
		BigDecimal marksObtained,
		BigDecimal maxMarks,
		String remarks) {
}
