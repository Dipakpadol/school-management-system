package com.school.erp.modules.attendance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.function.Function;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

public final class AttendanceSummaryCalculator {

	private static final BigDecimal HALF_DAY_WEIGHT = new BigDecimal("0.5");
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private AttendanceSummaryCalculator() {
	}

	public static <T> long count(
			Collection<T> records,
			AttendanceStatus status,
			Function<T, AttendanceStatus> statusAccessor) {
		return records.stream()
				.filter(record -> statusAccessor.apply(record) == status)
				.count();
	}

	public static <T> BigDecimal percentage(
			Collection<T> records,
			Function<T, AttendanceStatus> statusAccessor) {
		long total = records.size();
		if (total == 0) {
			return BigDecimal.ZERO;
		}
		return percentage(
				total,
				count(records, AttendanceStatus.PRESENT, statusAccessor),
				count(records, AttendanceStatus.LATE, statusAccessor),
				count(records, AttendanceStatus.HALF_DAY, statusAccessor));
	}

	public static BigDecimal percentage(long total, long present, long late, long halfDay) {
		if (total <= 0) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(present + late)
				.add(BigDecimal.valueOf(halfDay).multiply(HALF_DAY_WEIGHT))
				.multiply(HUNDRED)
				.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
	}
}
