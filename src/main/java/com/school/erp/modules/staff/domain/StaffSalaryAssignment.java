package com.school.erp.modules.staff.domain;

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
@Table(name = "staff_salary_assignments")
@SQLRestriction("deleted = false")
public class StaffSalaryAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "salary_structure_id", nullable = false)
	private SalaryStructure salaryStructure;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(nullable = false)
	private boolean active = true;

	public StaffSalaryAssignment(Staff staff, SalaryStructure salaryStructure, LocalDate effectiveFrom) {
		this.staff = staff;
		this.salaryStructure = salaryStructure;
		this.effectiveFrom = effectiveFrom;
	}

	public void close(LocalDate effectiveTo) {
		this.effectiveTo = effectiveTo;
		this.active = false;
	}
}
