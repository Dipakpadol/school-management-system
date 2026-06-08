package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StudentSubjectExamResultResponse(
		UUID examScheduleId,
		UUID subjectId,
		String subjectName,
		BigDecimal maxMarks,
		BigDecimal marksObtained,
		String grade,
		String remarks,
		LocalDate examDate) {
}
