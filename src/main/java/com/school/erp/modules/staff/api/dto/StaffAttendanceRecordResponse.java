package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

public record StaffAttendanceRecordResponse(
		UUID staffId,
		String employeeCode,
		String staffName,
		UUID departmentId,
		String departmentName,
		UUID designationId,
		String designationName,
		LocalDate date,
		AttendanceStatus status,
		String remarks,
		boolean approvedLeave) {
}
