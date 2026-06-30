package com.school.erp.modules.hostel.domain;

import java.math.BigDecimal;
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
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hostel_fee_structures")
@SQLRestriction("deleted = false")
public class HostelFeeStructure extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hostel_id", nullable = false)
	private Hostel hostel;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private HostelRoom room;

	@Column(name = "room_type", length = 80)
	private String roomType;

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

	public HostelFeeStructure(
			AcademicYear academicYear,
			Hostel hostel,
			HostelRoom room,
			String roomType,
			FeeCategory feeCategory,
			FeeStructure backingFeeStructure,
			BigDecimal amount,
			LocalDate dueDate,
			boolean installmentAllowed,
			int numberOfInstallments,
			FeeStructureStatus status) {
		update(
				academicYear,
				hostel,
				room,
				roomType,
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
			Hostel hostel,
			HostelRoom room,
			String roomType,
			FeeCategory feeCategory,
			FeeStructure backingFeeStructure,
			BigDecimal amount,
			LocalDate dueDate,
			boolean installmentAllowed,
			int numberOfInstallments,
			FeeStructureStatus status) {
		this.academicYear = academicYear;
		this.hostel = hostel;
		this.room = room;
		this.roomType = normalizeRoomType(roomType);
		this.feeCategory = feeCategory;
		this.backingFeeStructure = backingFeeStructure;
		this.amount = amount;
		this.dueDate = dueDate;
		this.installmentAllowed = installmentAllowed;
		this.numberOfInstallments = numberOfInstallments;
		this.status = status == null ? FeeStructureStatus.DRAFT : status;
	}

	public boolean isActive() {
		return status == FeeStructureStatus.ACTIVE;
	}

	private String normalizeRoomType(String value) {
		return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
	}
}
