package com.school.erp.modules.staff.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SalaryStructureResponse(
		UUID id,
		String code,
		String name,
		BigDecimal basicSalary,
		BigDecimal allowances,
		BigDecimal deductions,
		BigDecimal grossSalary,
		BigDecimal netSalary,
		boolean active) {
}
