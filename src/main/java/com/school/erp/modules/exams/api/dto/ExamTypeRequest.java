package com.school.erp.modules.exams.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamTypeRequest(
		@NotBlank @Size(max = 40) String code,
		@NotBlank @Size(max = 120) String name,
		@Size(max = 500) String description,
		int displayOrder,
		boolean active) {
}
