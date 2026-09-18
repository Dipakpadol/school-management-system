package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record StaffLeaveCreateRequest(
		@NotNull UUID staffId,
		@NotNull UUID leaveTypeId,
		@NotNull LocalDate startDate,
		@NotNull LocalDate endDate,
		String reason) {
}
