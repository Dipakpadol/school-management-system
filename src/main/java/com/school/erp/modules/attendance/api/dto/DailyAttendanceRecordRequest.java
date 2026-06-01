package com.school.erp.modules.attendance.api.dto;

import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DailyAttendanceRecordRequest(
		@NotNull UUID studentId,
		@NotNull AttendanceStatus status,
		@Size(max = 500) String remarks) {
}
