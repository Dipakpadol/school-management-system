package com.school.erp.modules.staff.domain;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "staff_designations")
@SQLRestriction("deleted = false")
public class Designation extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	private Department department;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	public Designation(String name, Department department, String description, boolean active) {
		update(name, department, description, active);
	}

	public void update(String name, Department department, String description, boolean active) {
		this.name = trim(name);
		this.department = department;
		this.description = trimToNull(description);
		this.active = active;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
