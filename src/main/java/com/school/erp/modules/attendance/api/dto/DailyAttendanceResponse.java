package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DailyAttendanceResponse(
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		LocalDate attendanceDate,
		int totalRecords,
		long presentCount,
		long absentCount,
		long lateCount,
		long halfDayCount,
		long leaveCount,
		List<AttendanceRecordResponse> records) {
}
