package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.exams.domain.ExamScheduleStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ExamScheduleRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID classId,
		@NotNull UUID sectionId,
		@NotNull UUID examTypeId,
		@Size(max = 160) String examName,
		@Valid List<ExamScheduleSubjectRequest> subjects,
		UUID subjectId,
		LocalDate examDate,
		LocalTime startTime,
		LocalTime endTime,
		@Size(max = 120) String room,
		@DecimalMin(value = "0.01") BigDecimal maxMarks,
		@DecimalMin(value = "0.00") BigDecimal passingMarks,
		ExamScheduleStatus status,
		@Size(max = 500) String description) {
}
