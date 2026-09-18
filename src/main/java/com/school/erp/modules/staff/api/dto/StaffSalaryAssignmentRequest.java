package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record StaffSalaryAssignmentRequest(
		@NotNull UUID staffId,
		@NotNull UUID salaryStructureId,
		@NotNull LocalDate effectiveFrom) {
}
