package com.school.erp.modules.attendance.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StudentAttendanceSummaryResponse(
		UUID studentId,
		String studentName,
		UUID academicYearId,
		long totalMarkedDays,
		long presentCount,
		long absentCount,
		long lateCount,
		long halfDayCount,
		long leaveCount,
		BigDecimal attendancePercentage) {
}
