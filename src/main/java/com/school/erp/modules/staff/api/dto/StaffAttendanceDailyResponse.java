package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.List;

public record StaffAttendanceDailyResponse(
		LocalDate date,
		long totalStaff,
		List<StaffAttendanceRecordResponse> records) {
}
