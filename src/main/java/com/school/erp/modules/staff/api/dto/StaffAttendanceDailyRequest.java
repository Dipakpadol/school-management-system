package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record StaffAttendanceDailyRequest(
		@NotNull LocalDate date,
		UUID departmentId,
		UUID designationId,
		@Valid List<StaffAttendanceEntryRequest> records) {
}
