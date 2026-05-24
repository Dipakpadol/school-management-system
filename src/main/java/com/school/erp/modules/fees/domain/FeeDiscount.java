package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fee_discounts")
@SQLRestriction("deleted = false")
public class FeeDiscount extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id", nullable = false)
	private StudentFeeAssignment assignment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "installment_id")
	private StudentFeeInstallment installment;

	@Enumerated(EnumType.STRING)
	@Column(name = "discount_type", nullable = false, length = 40)
	private DiscountType discountType;

	@Enumerated(EnumType.STRING)
	@Column(name = "calculation_type", nullable = false, length = 30)
	private DiscountCalculationType calculationType;

	@Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
	private BigDecimal value;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 500)
	private String reason;

	@Column(name = "approved_by", length = 100)
	private String approvedBy;

	@Column(name = "approved_at")
	private Instant approvedAt;

	public FeeDiscount(
			StudentFeeAssignment assignment,
			StudentFeeInstallment installment,
			DiscountType discountType,
			DiscountCalculationType calculationType,
			BigDecimal value,
			BigDecimal amount,
			String reason,
			String approvedBy) {
		this.assignment = assignment;
		this.installment = installment;
		this.discountType = discountType;
		this.calculationType = calculationType;
		this.value = StudentFeeAssignment.money(value);
		this.amount = StudentFeeAssignment.money(amount);
		this.reason = reason;
		this.approvedBy = trimToNull(approvedBy);
		this.approvedAt = Instant.now();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
