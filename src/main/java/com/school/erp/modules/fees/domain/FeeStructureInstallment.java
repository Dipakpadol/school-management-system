package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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
@Table(name = "fee_structure_installments")
@SQLRestriction("deleted = false")
public class FeeStructureInstallment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_structure_id", nullable = false)
	private FeeStructure feeStructure;

	@Column(name = "sequence_no", nullable = false)
	private int sequenceNo;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	FeeStructureInstallment(FeeStructure feeStructure, int sequenceNo, String title, LocalDate dueDate, BigDecimal amount) {
		this.feeStructure = feeStructure;
		this.sequenceNo = sequenceNo;
		this.title = title;
		this.dueDate = dueDate;
		this.amount = money(amount);
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}
}
