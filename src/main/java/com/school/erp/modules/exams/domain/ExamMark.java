package com.school.erp.modules.exams.domain;

import java.math.BigDecimal;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;
import com.school.erp.modules.students.domain.Student;

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
@Table(name = "exam_marks")
@SQLRestriction("deleted = false")
public class ExamMark extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "class_id", nullable = false)
	private ClassEntity classEntity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "section_id", nullable = false)
	private SectionEntity section;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exam_schedule_id", nullable = false)
	private ExamSchedule examSchedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(name = "marks_obtained", nullable = false, precision = 10, scale = 2)
	private BigDecimal marksObtained;

	@Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxMarks;

	@Column(length = 500)
	private String remarks;

	public ExamMark(
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			ExamSchedule examSchedule,
			Subject subject,
			Student student,
			BigDecimal marksObtained,
			BigDecimal maxMarks,
			String remarks) {
		this.academicYear = academicYear;
		this.classEntity = classEntity;
		this.section = section;
		this.examSchedule = examSchedule;
		this.subject = subject;
		this.student = student;
		update(marksObtained, maxMarks, remarks);
	}

	public void update(BigDecimal marksObtained, BigDecimal maxMarks, String remarks) {
		this.marksObtained = marksObtained;
		this.maxMarks = maxMarks;
		this.remarks = trimToNull(remarks);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
