package com.school.erp.modules.staff.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalaryStructureRequest(
		@NotBlank String code,
		@NotBlank String name,
		@NotNull @DecimalMin("0.00") BigDecimal basicSalary,
		@DecimalMin("0.00") BigDecimal allowances,
		@DecimalMin("0.00") BigDecimal deductions,
		Boolean active) {
}
