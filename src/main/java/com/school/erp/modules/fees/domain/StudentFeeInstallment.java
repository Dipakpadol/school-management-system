package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_fee_installments")
@SQLRestriction("deleted = false")
public class StudentFeeInstallment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id", nullable = false)
	private StudentFeeAssignment assignment;

	@Column(name = "sequence_no", nullable = false)
	private int sequenceNo;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Column(name = "late_fee_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal lateFeeAmount = BigDecimal.ZERO;

	@Column(name = "payable_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal payableAmount;

	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal balanceAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeeInstallmentStatus status = FeeInstallmentStatus.PENDING;

	StudentFeeInstallment(
			StudentFeeAssignment assignment,
			int sequenceNo,
			String title,
			LocalDate dueDate,
			BigDecimal amount) {
		this.assignment = assignment;
		this.sequenceNo = sequenceNo;
		this.title = title;
		this.dueDate = dueDate;
		this.amount = StudentFeeAssignment.money(amount);
		recalculate();
	}

	public boolean isSettledOrCancelled() {
		return status == FeeInstallmentStatus.PAID || status == FeeInstallmentStatus.CANCELLED;
	}

	public boolean isOverdue() {
		return status == FeeInstallmentStatus.OVERDUE;
	}

	public BigDecimal discountableBalance() {
		return amount.subtract(discountAmount).max(BigDecimal.ZERO);
	}

	public void applyDiscount(BigDecimal amount) {
		discountAmount = discountAmount.add(StudentFeeAssignment.money(amount));
		recalculate();
	}

	public void assessLateFee(BigDecimal amount) {
		lateFeeAmount = StudentFeeAssignment.money(amount);
		recalculate();
	}

	public void applyPayment(BigDecimal amount) {
		paidAmount = paidAmount.add(StudentFeeAssignment.money(amount));
		recalculate();
	}

	public void cancel() {
		status = FeeInstallmentStatus.CANCELLED;
	}

	private void recalculate() {
		payableAmount = amount.subtract(discountAmount).add(lateFeeAmount).max(BigDecimal.ZERO);
		balanceAmount = payableAmount.subtract(paidAmount).max(BigDecimal.ZERO);
		if (status == FeeInstallmentStatus.CANCELLED) {
			return;
		}
		if (balanceAmount.signum() == 0) {
			status = FeeInstallmentStatus.PAID;
		}
		else if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
			status = paidAmount.signum() > 0 ? FeeInstallmentStatus.PARTIALLY_PAID : FeeInstallmentStatus.OVERDUE;
		}
		else if (paidAmount.signum() > 0) {
			status = FeeInstallmentStatus.PARTIALLY_PAID;
		}
		else {
			status = FeeInstallmentStatus.PENDING;
		}
	}
}
