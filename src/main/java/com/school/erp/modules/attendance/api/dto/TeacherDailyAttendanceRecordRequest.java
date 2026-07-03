package com.school.erp.modules.attendance.api.dto;

import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Teacher attendance record in a daily attendance save request.")
public record TeacherDailyAttendanceRecordRequest(
		@NotNull UUID teacherId,
		@NotNull AttendanceStatus status,
		@Size(max = 500) String remarks) {
}
