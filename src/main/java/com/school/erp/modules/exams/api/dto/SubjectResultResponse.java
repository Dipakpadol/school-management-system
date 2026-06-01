package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubjectResultResponse(
		UUID subjectId,
		String subjectName,
		BigDecimal marksObtained,
		BigDecimal maxMarks,
		String grade,
		boolean passed) {
}
