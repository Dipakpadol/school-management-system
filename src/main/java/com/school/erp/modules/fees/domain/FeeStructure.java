package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;

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
@Table(name = "fee_structures")
@SQLRestriction("deleted = false")
public class FeeStructure extends BaseEntity {

	@Column(name = "academic_year", nullable = false, length = 20)
	private String academicYear;

	@Column(name = "class_name", nullable = false, length = 80)
	private String className;

	@Column(name = "section_name", length = 80)
	private String sectionName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "academic_year_id")
	private AcademicYear academicYearEntity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private ClassEntity classEntity;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeeStructureStatus status = FeeStructureStatus.DRAFT;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@OneToMany(mappedBy = "feeStructure", cascade = CascadeType.ALL)
	private Set<FeeStructureItem> items = new LinkedHashSet<>();

	@OneToMany(mappedBy = "feeStructure", cascade = CascadeType.ALL)
	private Set<FeeStructureInstallment> installments = new LinkedHashSet<>();

	public FeeStructure(
			String academicYear,
			String className,
			String sectionName,
			String name,
			String description) {
		this.academicYear = trim(academicYear);
		this.className = trim(className);
		this.sectionName = trimToNull(sectionName);
		this.name = name;
		this.description = trimToNull(description);
	}

	public FeeStructureItem addItem(FeeCategory category, BigDecimal amount, boolean mandatory, int sortOrder) {
		FeeStructureItem item = new FeeStructureItem(this, category, amount, mandatory, sortOrder);
		items.add(item);
		recalculateTotal();
		return item;
	}

	public FeeStructureInstallment addInstallment(int sequenceNo, String title, LocalDate dueDate, BigDecimal amount) {
		FeeStructureInstallment installment = new FeeStructureInstallment(this, sequenceNo, title, dueDate, amount);
		installments.add(installment);
		return installment;
	}

	public void updateDetails(
			String academicYear,
			String className,
			String sectionName,
			String name,
			String description) {
		this.academicYear = trim(academicYear);
		this.className = trim(className);
		this.sectionName = trimToNull(sectionName);
		this.name = trim(name);
		this.description = trimToNull(description);
	}

	public void updateAcademicMapping(AcademicYear academicYearEntity, ClassEntity classEntity) {
		this.academicYearEntity = academicYearEntity;
		this.classEntity = classEntity;
		if (academicYearEntity != null) {
			this.academicYear = academicYearEntity.getName();
		}
		if (classEntity != null) {
			this.className = classEntity.getName();
		}
	}

	public void clearItems(String actor) {
		items.forEach(item -> item.softDelete(actor));
		items.clear();
		recalculateTotal();
	}

	public void clearInstallments(String actor) {
		installments.forEach(installment -> installment.softDelete(actor));
		installments.clear();
	}

	public void activate() {
		status = FeeStructureStatus.ACTIVE;
	}

	public void deactivate() {
		status = FeeStructureStatus.INACTIVE;
	}

	public boolean isActive() {
		return status == FeeStructureStatus.ACTIVE;
	}

	public BigDecimal installmentTotal() {
		return installments.stream()
				.map(FeeStructureInstallment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Set<FeeStructureInstallment> orderedInstallments() {
		return installments.stream()
				.sorted(Comparator.comparingInt(FeeStructureInstallment::getSequenceNo))
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private void recalculateTotal() {
		totalAmount = items.stream()
				.map(FeeStructureItem::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
