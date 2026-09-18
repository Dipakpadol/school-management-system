package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record StaffSalaryAssignmentResponse(
		UUID id,
		UUID staffId,
		String employeeCode,
		String staffName,
		UUID salaryStructureId,
		String salaryStructureName,
		LocalDate effectiveFrom,
		LocalDate effectiveTo,
		boolean active) {
}
