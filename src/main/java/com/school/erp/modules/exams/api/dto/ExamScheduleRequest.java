package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.exams.domain.ExamScheduleStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamScheduleRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID classId,
		@NotNull UUID sectionId,
		@NotNull UUID examTypeId,
		@NotNull UUID subjectId,
		@NotNull LocalDate examDate,
		@NotNull @DecimalMin(value = "0.01") BigDecimal maxMarks,
		ExamScheduleStatus status,
		@Size(max = 500) String description) {
}
