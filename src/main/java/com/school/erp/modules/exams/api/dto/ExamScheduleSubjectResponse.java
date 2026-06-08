package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ExamScheduleSubjectResponse(
		UUID id,
		UUID subjectId,
		String subjectName,
		LocalDate examDate,
		BigDecimal maxMarks,
		BigDecimal passingMarks) {
}
