package com.school.erp.modules.staff.domain;

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
@Table(name = "staff_departments")
@SQLRestriction("deleted = false")
public class Department extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	public Department(String name, String description, boolean active) {
		update(name, description, active);
	}

	public void update(String name, String description, boolean active) {
		this.name = trim(name);
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
