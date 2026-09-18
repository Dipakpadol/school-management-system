package com.school.erp.modules.staff.api.dto;

import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

import jakarta.validation.constraints.NotNull;

public record StaffAttendanceEntryRequest(
		@NotNull UUID staffId,
		@NotNull AttendanceStatus status,
		String remarks) {
}
