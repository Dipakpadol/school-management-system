package com.school.erp.modules.transport.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureStatus;

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
@Table(name = "transport_fee_structures")
@SQLRestriction("deleted = false")
public class TransportFeeStructure extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private TransportRoute route;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pickup_point_id")
	private TransportPickupPoint pickupPoint;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_category_id", nullable = false)
	private FeeCategory feeCategory;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "backing_fee_structure_id", nullable = false)
	private FeeStructure backingFeeStructure;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "installment_allowed", nullable = false)
	private boolean installmentAllowed;

	@Column(name = "number_of_installments", nullable = false)
	private int numberOfInstallments = 1;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeeStructureStatus status = FeeStructureStatus.DRAFT;

	public TransportFeeStructure(
			AcademicYear academicYear,
			TransportRoute route,
			TransportPickupPoint pickupPoint,
			FeeCategory feeCategory,
			FeeStructure backingFeeStructure,
			BigDecimal amount,
			LocalDate dueDate,
			boolean installmentAllowed,
			int numberOfInstallments,
			FeeStructureStatus status) {
		update(
				academicYear,
				route,
				pickupPoint,
				feeCategory,
				backingFeeStructure,
				amount,
				dueDate,
				installmentAllowed,
				numberOfInstallments,
				status);
	}

	public void update(
			AcademicYear academicYear,
			TransportRoute route,
			TransportPickupPoint pickupPoint,
			FeeCategory feeCategory,
			FeeStructure backingFeeStructure,
			BigDecimal amount,
			LocalDate dueDate,
			boolean installmentAllowed,
			int numberOfInstallments,
			FeeStructureStatus status) {
		this.academicYear = academicYear;
		this.route = route;
		this.pickupPoint = pickupPoint;
		this.feeCategory = feeCategory;
		this.backingFeeStructure = backingFeeStructure;
		this.amount = money(amount);
		this.dueDate = dueDate;
		this.installmentAllowed = installmentAllowed;
		this.numberOfInstallments = Math.max(numberOfInstallments, 1);
		this.status = status == null ? FeeStructureStatus.DRAFT : status;
	}

	public boolean isActive() {
		return status == FeeStructureStatus.ACTIVE;
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}
}
