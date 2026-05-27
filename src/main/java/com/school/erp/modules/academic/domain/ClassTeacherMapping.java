package com.school.erp.modules.academic.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "class_teacher_mapping")
@SQLRestriction("deleted = false")
public class ClassTeacherMapping extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassEntity classEntity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private SectionEntity section;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@Column(nullable = false)
	private boolean active = true;

	public ClassTeacherMapping(
			ClassEntity classEntity,
			SectionEntity section,
			Teacher teacher,
			LocalDate effectiveFrom) {
		this.classEntity = classEntity;
		this.section = section;
		this.teacher = teacher;
		this.effectiveFrom = effectiveFrom;
	}

	public void deactivate(LocalDate effectiveTo) {
		active = false;
		this.effectiveTo = effectiveTo;
	}
}
