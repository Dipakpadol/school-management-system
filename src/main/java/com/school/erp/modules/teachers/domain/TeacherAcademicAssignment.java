package com.school.erp.modules.teachers.domain;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.academic.domain.Teacher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "teacher_academic_assignments")
@SQLRestriction("deleted = false")
public class TeacherAcademicAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@Enumerated(EnumType.STRING)
	@Column(name = "assignment_type", nullable = false, length = 40)
	private TeacherAssignmentType assignmentType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class_id")
	private ClassEntity classEntity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "section_id")
	private SectionEntity section;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subject_id")
	private Subject subject;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TeacherAssignmentStatus status = TeacherAssignmentStatus.ACTIVE;

	public TeacherAcademicAssignment(
			Teacher teacher,
			AcademicYear academicYear,
			TeacherAssignmentType assignmentType,
			ClassEntity classEntity,
			SectionEntity section,
			Subject subject,
			TeacherAssignmentStatus status) {
		update(teacher, academicYear, assignmentType, classEntity, section, subject, status);
	}

	public boolean isActive() {
		return status == TeacherAssignmentStatus.ACTIVE;
	}

	public void update(
			Teacher teacher,
			AcademicYear academicYear,
			TeacherAssignmentType assignmentType,
			ClassEntity classEntity,
			SectionEntity section,
			Subject subject,
			TeacherAssignmentStatus status) {
		this.teacher = teacher;
		this.academicYear = academicYear;
		this.assignmentType = assignmentType;
		this.classEntity = classEntity;
		this.section = section;
		this.subject = subject;
		this.status = status == null ? TeacherAssignmentStatus.ACTIVE : status;
	}
}
