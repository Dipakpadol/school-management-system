package com.school.erp.modules.staff.domain;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "payroll_records")
@SQLRestriction("deleted = false")
public class PayrollRecord extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@Column(name = "payroll_year", nullable = false)
	private int payrollYear;

	@Column(name = "payroll_month", nullable = false)
	private int payrollMonth;

	@Column(name = "salary_structure_name", nullable = false, length = 140)
	private String salaryStructureName;

	@Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal basicSalary;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal allowances;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal deductions;

	@Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossSalary;

	@Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal netSalary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PayrollStatus status = PayrollStatus.PENDING;

	@Column(name = "generated_at", nullable = false)
	private Instant generatedAt = Instant.now();

	@Column(name = "paid_at")
	private Instant paidAt;

	public PayrollRecord(
			Staff staff,
			int payrollYear,
			int payrollMonth,
			SalaryStructure salaryStructure) {
		this.staff = staff;
		this.payrollYear = payrollYear;
		this.payrollMonth = payrollMonth;
		this.salaryStructureName = salaryStructure.getName();
		this.basicSalary = salaryStructure.getBasicSalary();
		this.allowances = salaryStructure.getAllowances();
		this.deductions = salaryStructure.getDeductions();
		this.grossSalary = salaryStructure.grossSalary();
		this.netSalary = salaryStructure.netSalary();
	}

	public void markReviewed() {
		if (status == PayrollStatus.PENDING) {
			status = PayrollStatus.REVIEWED;
		}
	}

	public void markPaid() {
		status = PayrollStatus.PAID;
		paidAt = Instant.now();
	}
}
