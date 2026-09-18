package com.school.erp.modules.staff.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.staff.domain.PayrollStatus;

public record PayrollRecordResponse(
		UUID id,
		UUID staffId,
		String employeeCode,
		String staffName,
		int payrollYear,
		int payrollMonth,
		String salaryStructureName,
		BigDecimal basicSalary,
		BigDecimal allowances,
		BigDecimal deductions,
		BigDecimal grossSalary,
		BigDecimal netSalary,
		PayrollStatus status,
		Instant generatedAt,
		Instant paidAt) {
}
