package com.school.erp.modules.attendance.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher attendance history row.")
public record TeacherAttendanceHistoryRecordResponse(
		LocalDate attendanceDate,
		AttendanceStatus status,
		String remarks,
		String markedBy,
		Instant markedAt,
		String updatedBy,
		Instant updatedAt) {
}
