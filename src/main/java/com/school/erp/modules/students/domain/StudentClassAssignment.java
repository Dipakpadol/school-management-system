package com.school.erp.modules.students.domain;

import java.time.LocalDate;

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
@Table(name = "student_class_assignments")
@SQLRestriction("deleted = false")
public class StudentClassAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(name = "academic_year", nullable = false, length = 20)
	private String academicYear;

	@Column(name = "class_name", nullable = false, length = 80)
	private String className;

	@Column(name = "section_name", nullable = false, length = 80)
	private String sectionName;

	@Column(name = "roll_number", length = 30)
	private String rollNumber;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(nullable = false)
	private boolean active = true;

	StudentClassAssignment(
			Student student,
			String academicYear,
			String className,
			String sectionName,
			String rollNumber,
			LocalDate effectiveFrom) {
		this.student = student;
		this.academicYear = academicYear;
		this.className = className;
		this.sectionName = sectionName;
		this.rollNumber = trimToNull(rollNumber);
		this.effectiveFrom = effectiveFrom;
	}

	public void deactivate(LocalDate effectiveTo) {
		active = false;
		this.effectiveTo = effectiveTo;
	}

	public void update(
			String academicYear,
			String className,
			String sectionName,
			String rollNumber,
			LocalDate effectiveFrom,
			LocalDate effectiveTo,
			boolean active) {
		this.academicYear = academicYear;
		this.className = className;
		this.sectionName = sectionName;
		this.rollNumber = trimToNull(rollNumber);
		this.effectiveFrom = effectiveFrom;
		this.effectiveTo = effectiveTo;
		this.active = active;
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
