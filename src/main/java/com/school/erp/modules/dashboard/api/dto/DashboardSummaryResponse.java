package com.school.erp.modules.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Operational dashboard summary backed by live School ERP data.")
public record DashboardSummaryResponse(
		long totalStudents,
		long totalStaff,
		long totalTeachers,
		long totalParents,
		long totalUsers,
		long activeUsers,
		long inactiveUsers,
		BigDecimal todayAttendancePercentage,
		BigDecimal totalFeeCollected,
		BigDecimal pendingFeeAmount,
		List<RecentActivityResponse> recentActivities,
		List<DashboardNotificationResponse> notifications,
		List<BirthdayTodayResponse> birthdaysToday) {

	public DashboardSummaryResponse {
		todayAttendancePercentage = money(todayAttendancePercentage);
		totalFeeCollected = money(totalFeeCollected);
		pendingFeeAmount = money(pendingFeeAmount);
		recentActivities = recentActivities == null ? List.of() : List.copyOf(recentActivities);
		notifications = notifications == null ? List.of() : List.copyOf(notifications);
		birthdaysToday = birthdaysToday == null ? List.of() : List.copyOf(birthdaysToday);
	}

	public record RecentActivityResponse(
			UUID id,
			String moduleName,
			String entityName,
			String entityId,
			String action,
			String performedBy,
			Instant performedAt) {
	}

	public record DashboardNotificationResponse(
			UUID id,
			String title,
			String message,
			Instant createdAt) {
	}

	public record BirthdayTodayResponse(
			UUID studentId,
			String studentName,
			LocalDate dateOfBirth,
			String className,
			String sectionName) {
	}

	private static BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
