package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ExamScheduleSubjectResponse(
		UUID id,
		UUID subjectId,
		String subjectName,
		LocalDate examDate,
		LocalTime startTime,
		LocalTime endTime,
		String room,
		BigDecimal maxMarks,
		BigDecimal passingMarks) {
}
