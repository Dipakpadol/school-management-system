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
@Table(name = "staff_leave_types")
@SQLRestriction("deleted = false")
public class LeaveType extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean paid;

	@Column(nullable = false)
	private boolean active = true;

	public LeaveType(String name, String description, boolean paid, boolean active) {
		update(name, description, paid, active);
	}

	public void update(String name, String description, boolean paid, boolean active) {
		this.name = trim(name);
		this.description = trimToNull(description);
		this.paid = paid;
		this.active = active;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
