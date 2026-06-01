package com.school.erp.modules.exams.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Subject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "exam_schedules")
@SQLRestriction("deleted = false")
public class ExamSchedule extends BaseEntity {

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
	@JoinColumn(name = "exam_type_id", nullable = false)
	private ExamType examType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@Column(name = "exam_date", nullable = false)
	private LocalDate examDate;

	@Column(name = "max_marks", nullable = false, precision = 10, scale = 2)
	private BigDecimal maxMarks;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ExamScheduleStatus status = ExamScheduleStatus.SCHEDULED;

	@Column(length = 500)
	private String description;

	public ExamSchedule(
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			ExamType examType,
			Subject subject,
			LocalDate examDate,
			BigDecimal maxMarks,
			ExamScheduleStatus status,
			String description) {
		this.academicYear = academicYear;
		this.classEntity = classEntity;
		this.section = section;
		this.examType = examType;
		this.subject = subject;
		update(examDate, maxMarks, status, description);
	}

	public void update(LocalDate examDate, BigDecimal maxMarks, ExamScheduleStatus status, String description) {
		this.examDate = examDate;
		this.maxMarks = maxMarks;
		this.status = status == null ? ExamScheduleStatus.SCHEDULED : status;
		this.description = trimToNull(description);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
