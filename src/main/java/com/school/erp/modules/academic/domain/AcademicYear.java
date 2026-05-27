package com.school.erp.modules.academic.domain;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "academic_years")
@SQLRestriction("deleted = false")
public class AcademicYear extends BaseEntity {

	@Column(nullable = false, length = 40)
	private String code;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "academicYear", cascade = CascadeType.ALL)
	private Set<ClassEntity> classes = new LinkedHashSet<>();

	public AcademicYear(String code, String name, LocalDate startDate, LocalDate endDate) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public void update(String code, String name, LocalDate startDate, LocalDate endDate, boolean active) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.startDate = startDate;
		this.endDate = endDate;
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
