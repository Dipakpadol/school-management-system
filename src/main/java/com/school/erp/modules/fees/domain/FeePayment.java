package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fee_payments")
@SQLRestriction("deleted = false")
public class FeePayment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id", nullable = false)
	private StudentFeeAssignment assignment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "receipt_id", nullable = false)
	private FeeReceipt receipt;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_mode", nullable = false, length = 30)
	private PaymentMode paymentMode;

	@Column(name = "reference_number", length = 120)
	private String referenceNumber;

	@Column(name = "payer_name", nullable = false, length = 160)
	private String payerName;

	@Column(name = "collected_by", length = 100)
	private String collectedBy;

	@Column(length = 500)
	private String remarks;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeePaymentStatus status = FeePaymentStatus.COMPLETED;

	@OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
	private Set<FeePaymentAllocation> allocations = new LinkedHashSet<>();

	public FeePayment(
			StudentFeeAssignment assignment,
			FeeReceipt receipt,
			BigDecimal amount,
			LocalDate paymentDate,
			PaymentMode paymentMode,
			String referenceNumber,
			String payerName,
			String collectedBy,
			String remarks) {
		this.assignment = assignment;
		this.receipt = receipt;
		this.amount = StudentFeeAssignment.money(amount);
		this.paymentDate = paymentDate;
		this.paymentMode = paymentMode;
		this.referenceNumber = trimToNull(referenceNumber);
		this.payerName = payerName;
		this.collectedBy = trimToNull(collectedBy);
		this.remarks = trimToNull(remarks);
	}

	void addAllocation(StudentFeeInstallment installment, BigDecimal amount) {
		allocations.add(new FeePaymentAllocation(this, installment, amount));
	}

	public void cancel() {
		status = FeePaymentStatus.CANCELLED;
	}

	public boolean isCompleted() {
		return status == FeePaymentStatus.COMPLETED;
	}

	public void reverse(String reason) {
		updateStatus(FeePaymentStatus.REVERSED, reason);
	}

	public void voidPayment(String reason) {
		updateStatus(FeePaymentStatus.VOIDED, reason);
	}

	public void refund(String reason) {
		updateStatus(FeePaymentStatus.REFUNDED, reason);
	}

	private void updateStatus(FeePaymentStatus status, String reason) {
		this.status = status;
		if (StringUtils.hasText(reason)) {
			String suffix = status.name() + ": " + reason.trim();
			this.remarks = StringUtils.hasText(this.remarks) ? this.remarks + " | " + suffix : suffix;
		}
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
