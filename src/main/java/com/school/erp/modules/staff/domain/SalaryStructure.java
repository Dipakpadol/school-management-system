package com.school.erp.modules.staff.domain;

import java.math.BigDecimal;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "salary_structures")
@SQLRestriction("deleted = false")
public class SalaryStructure extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 140)
	private String name;

	@Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
	private BigDecimal basicSalary = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal allowances = BigDecimal.ZERO;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal deductions = BigDecimal.ZERO;

	@Column(nullable = false)
	private boolean active = true;

	public SalaryStructure(
			String code,
			String name,
			BigDecimal basicSalary,
			BigDecimal allowances,
			BigDecimal deductions,
			boolean active) {
		update(code, name, basicSalary, allowances, deductions, active);
	}

	public void update(
			String code,
			String name,
			BigDecimal basicSalary,
			BigDecimal allowances,
			BigDecimal deductions,
			boolean active) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.basicSalary = money(basicSalary);
		this.allowances = money(allowances);
		this.deductions = money(deductions);
		this.active = active;
	}

	public BigDecimal grossSalary() {
		return basicSalary.add(allowances);
	}

	public BigDecimal netSalary() {
		return grossSalary().subtract(deductions);
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
