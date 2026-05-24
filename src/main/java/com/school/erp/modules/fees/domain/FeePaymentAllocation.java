package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "fee_payment_allocations")
@SQLRestriction("deleted = false")
public class FeePaymentAllocation extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "payment_id", nullable = false)
	private FeePayment payment;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "installment_id", nullable = false)
	private StudentFeeInstallment installment;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	FeePaymentAllocation(FeePayment payment, StudentFeeInstallment installment, BigDecimal amount) {
		this.payment = payment;
		this.installment = installment;
		this.amount = StudentFeeAssignment.money(amount);
	}
}
