package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
@Table(name = "fee_structure_items")
@SQLRestriction("deleted = false")
public class FeeStructureItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_structure_id", nullable = false)
	private FeeStructure feeStructure;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_category_id", nullable = false)
	private FeeCategory category;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private boolean mandatory = true;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	FeeStructureItem(FeeStructure feeStructure, FeeCategory category, BigDecimal amount, boolean mandatory, int sortOrder) {
		this.feeStructure = feeStructure;
		this.category = category;
		this.amount = money(amount);
		this.mandatory = mandatory;
		this.sortOrder = sortOrder;
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}
}
