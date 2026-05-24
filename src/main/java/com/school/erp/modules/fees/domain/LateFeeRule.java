package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "late_fee_rules")
@SQLRestriction("deleted = false")
public class LateFeeRule extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "academic_year", nullable = false, length = 20)
	private String academicYear;

	@Column(name = "class_name", length = 80)
	private String className;

	@Column(name = "section_name", length = 80)
	private String sectionName;

	@Column(name = "grace_days", nullable = false)
	private int graceDays;

	@Enumerated(EnumType.STRING)
	@Column(name = "calculation_type", nullable = false, length = 30)
	private LateFeeCalculationType calculationType;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "max_amount", precision = 12, scale = 2)
	private BigDecimal maxAmount;

	@Column(nullable = false)
	private boolean active = true;

	public LateFeeRule(
			String name,
			String academicYear,
			String className,
			String sectionName,
			int graceDays,
			LateFeeCalculationType calculationType,
			BigDecimal amount,
			BigDecimal maxAmount,
			boolean active) {
		this.name = name;
		this.academicYear = academicYear;
		this.className = trimToNull(className);
		this.sectionName = trimToNull(sectionName);
		this.graceDays = graceDays;
		this.calculationType = calculationType;
		this.amount = money(amount);
		this.maxAmount = maxAmount == null ? null : money(maxAmount);
		this.active = active;
	}

	public boolean appliesTo(StudentFeeAssignment assignment) {
		return active
				&& academicYear.equalsIgnoreCase(assignment.getAcademicYear())
				&& matchesOptional(className, assignment.getClassName())
				&& matchesOptional(sectionName, assignment.getSectionName());
	}

	public BigDecimal calculate(StudentFeeInstallment installment, LocalDate asOf) {
		LocalDate chargeFrom = installment.getDueDate().plusDays(graceDays);
		if (asOf == null || !asOf.isAfter(chargeFrom)) {
			return BigDecimal.ZERO;
		}
		long overdueDays = Math.max(1, ChronoUnit.DAYS.between(chargeFrom, asOf));
		BigDecimal fee = switch (calculationType) {
			case FLAT -> amount;
			case PER_DAY -> amount.multiply(BigDecimal.valueOf(overdueDays));
			case PERCENTAGE -> installment.getBalanceAmount()
					.multiply(amount)
					.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		};
		if (maxAmount != null) {
			fee = fee.min(maxAmount);
		}
		return money(fee);
	}

	public void update(
			String name,
			String academicYear,
			String className,
			String sectionName,
			int graceDays,
			LateFeeCalculationType calculationType,
			BigDecimal amount,
			BigDecimal maxAmount,
			boolean active) {
		this.name = name;
		this.academicYear = academicYear;
		this.className = trimToNull(className);
		this.sectionName = trimToNull(sectionName);
		this.graceDays = graceDays;
		this.calculationType = calculationType;
		this.amount = money(amount);
		this.maxAmount = maxAmount == null ? null : money(maxAmount);
		this.active = active;
	}

	private boolean matchesOptional(String configured, String actual) {
		return !StringUtils.hasText(configured) || configured.equalsIgnoreCase(actual);
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
