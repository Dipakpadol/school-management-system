package com.school.erp.modules.fees.domain;

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
@Table(name = "fee_categories")
@SQLRestriction("deleted = false")
public class FeeCategory extends BaseEntity {

	@Column(nullable = false, unique = true, length = 60)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean active = true;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_mandatory", nullable = false)
	private boolean mandatory = true;

	public FeeCategory(String code, String name, String description, int sortOrder) {
		this.code = normalizeCode(code);
		this.name = name;
		this.description = trimToNull(description);
		this.sortOrder = sortOrder;
	}

	public void update(String code, String name, String description, boolean active, int sortOrder, boolean mandatory) {
		this.code = normalizeCode(code);
		this.name = name;
		this.description = trimToNull(description);
		this.active = active;
		this.sortOrder = sortOrder;
		this.mandatory = mandatory;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
