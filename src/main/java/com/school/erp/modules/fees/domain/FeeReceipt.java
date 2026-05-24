package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.students.domain.Student;

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
@Table(name = "fee_receipts")
@SQLRestriction("deleted = false")
public class FeeReceipt extends BaseEntity {

	@Column(name = "receipt_number", nullable = false, unique = true, length = 40)
	private String receiptNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assignment_id", nullable = false)
	private StudentFeeAssignment assignment;

	@Column(name = "receipt_date", nullable = false)
	private Instant receiptDate;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "payer_name", nullable = false, length = 160)
	private String payerName;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_mode", nullable = false, length = 30)
	private PaymentMode paymentMode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeeReceiptStatus status = FeeReceiptStatus.ISSUED;

	public FeeReceipt(
			String receiptNumber,
			Student student,
			StudentFeeAssignment assignment,
			BigDecimal totalAmount,
			String payerName,
			PaymentMode paymentMode) {
		this.receiptNumber = receiptNumber;
		this.student = student;
		this.assignment = assignment;
		this.receiptDate = Instant.now();
		this.totalAmount = StudentFeeAssignment.money(totalAmount);
		this.payerName = payerName;
		this.paymentMode = paymentMode;
	}

	public void cancel() {
		status = FeeReceiptStatus.CANCELLED;
	}
}
