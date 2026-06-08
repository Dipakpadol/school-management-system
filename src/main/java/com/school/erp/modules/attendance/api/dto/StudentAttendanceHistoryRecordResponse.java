package com.school.erp.modules.attendance.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

public record StudentAttendanceHistoryRecordResponse(
		LocalDate attendanceDate,
		AttendanceStatus status,
		String remarks,
		String markedBy,
		Instant markedAt,
		String updatedBy,
		Instant updatedAt) {
}
