package com.school.erp.modules.exams.api.dto;

import java.util.UUID;

public record ExamTypeResponse(
		UUID id,
		String code,
		String name,
		String description,
		int displayOrder,
		boolean active) {
}
