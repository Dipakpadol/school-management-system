package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamScheduleSubjectRequest(
		@NotNull UUID subjectId,
		@NotNull LocalDate examDate,
		LocalTime startTime,
		LocalTime endTime,
		@Size(max = 120) String room,
		@NotNull @DecimalMin(value = "0.01") BigDecimal maxMarks,
		@DecimalMin(value = "0.00") BigDecimal passingMarks) {
}
