package com.school.erp.modules.exams.domain;

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
@Table(name = "exam_types")
@SQLRestriction("deleted = false")
public class ExamType extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private boolean active = true;

	public ExamType(String code, String name, String description, int displayOrder, boolean active) {
		update(code, name, description, displayOrder, active);
	}

	public void update(String code, String name, String description, int displayOrder, boolean active) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.description = trimToNull(description);
		this.displayOrder = displayOrder;
		this.active = active;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
