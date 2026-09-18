package com.school.erp.modules.staff.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.staff.domain.LeaveStatus;

public record StaffLeaveResponse(
		UUID id,
		UUID staffId,
		String employeeCode,
		String staffName,
		UUID leaveTypeId,
		String leaveTypeName,
		LocalDate startDate,
		LocalDate endDate,
		long durationDays,
		String reason,
		LeaveStatus status,
		String requestedBy,
		String reviewedBy,
		Instant reviewedAt,
		String reviewComment) {
}
