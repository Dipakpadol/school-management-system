package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record StaffExitRequest(
		@NotNull LocalDate relievingDate,
		String reason) {
}
