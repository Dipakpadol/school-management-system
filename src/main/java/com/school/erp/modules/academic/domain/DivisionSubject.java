package com.school.erp.modules.academic.domain;

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
@Table(name = "division_subjects")
@SQLRestriction("deleted = false")
public class DivisionSubject extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private SectionEntity section;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "teacher_id")
	private Teacher teacher;

	@Column(nullable = false)
	private boolean active = true;

	public DivisionSubject(SectionEntity section, Subject subject, Teacher teacher) {
		this.section = section;
		this.subject = subject;
		this.teacher = teacher;
	}

	public void update(Teacher teacher, boolean active) {
		this.teacher = teacher;
		this.active = active;
	}

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
	}
}
