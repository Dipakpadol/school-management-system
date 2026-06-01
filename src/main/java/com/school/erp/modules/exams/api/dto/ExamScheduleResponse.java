package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.exams.domain.ExamScheduleStatus;

public record ExamScheduleResponse(
		UUID id,
		UUID academicYearId,
		String academicYearName,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		UUID examTypeId,
		String examTypeName,
		UUID subjectId,
		String subjectName,
		LocalDate examDate,
		BigDecimal maxMarks,
		ExamScheduleStatus status,
		String description) {
}
