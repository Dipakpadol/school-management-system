package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ExamScheduleSubjectRequest(
		@NotNull UUID subjectId,
		@NotNull LocalDate examDate,
		@NotNull @DecimalMin(value = "0.01") BigDecimal maxMarks,
		@DecimalMin(value = "0.00") BigDecimal passingMarks) {
}
