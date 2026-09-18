package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayrollGenerateRequest(
		@NotNull UUID staffId,
		@Min(2000) @Max(2100) int payrollYear,
		@Min(1) @Max(12) int payrollMonth) {
}
