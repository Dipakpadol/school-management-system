package com.school.erp.modules.exams.api.dto;

import java.util.UUID;

public record ExamStudentResponse(
		UUID studentId,
		String admissionNumber,
		String rollNumber,
		String displayName) {
}
