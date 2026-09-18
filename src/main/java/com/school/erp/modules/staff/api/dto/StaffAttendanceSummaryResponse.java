package com.school.erp.modules.staff.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record StaffAttendanceSummaryResponse(
		UUID departmentId,
		String departmentName,
		UUID designationId,
		String designationName,
		LocalDate fromDate,
		LocalDate toDate,
		long eligibleStaffCount,
		long totalRecords,
		long presentCount,
		long absentCount,
		long lateCount,
		long halfDayCount,
		long leaveCount,
		BigDecimal attendancePercentage) {
}
