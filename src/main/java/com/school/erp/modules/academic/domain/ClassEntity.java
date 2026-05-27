package com.school.erp.modules.academic.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "classes")
@SQLRestriction("deleted = false")
public class ClassEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "classEntity", cascade = CascadeType.ALL)
	private Set<SectionEntity> sections = new LinkedHashSet<>();

	public ClassEntity(AcademicYear academicYear, String code, String name, int displayOrder) {
		this.academicYear = academicYear;
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.displayOrder = displayOrder;
	}

	public void update(String code, String name, int displayOrder, boolean active) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.displayOrder = displayOrder;
		this.active = active;
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
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
}
