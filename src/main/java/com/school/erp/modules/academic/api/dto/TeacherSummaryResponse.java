package com.school.erp.modules.academic.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher summary visible from class/division mapping.")
public record TeacherSummaryResponse(
		UUID id,
		String employeeNumber,
		String displayName,
		String email,
		String phoneNumber) {
}
