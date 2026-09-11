package com.school.erp.modules.exams.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
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

	@Column(name = "exam_name", length = 160)
	private String examName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "subject_id")
	private Subject subject;

	@Column(name = "exam_date")
	private LocalDate examDate;

	@Column(name = "max_marks", precision = 10, scale = 2)
	private BigDecimal maxMarks;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ExamScheduleStatus status = ExamScheduleStatus.SCHEDULED;

	@Column(length = 500)
	private String description;

	@OneToMany(mappedBy = "examSchedule", cascade = CascadeType.ALL)
	private List<ExamScheduleSubject> subjects = new ArrayList<>();

	public ExamSchedule(
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			ExamType examType,
			String examName,
			ExamScheduleStatus status,
			String description) {
		this.academicYear = academicYear;
		this.classEntity = classEntity;
		this.section = section;
		this.examType = examType;
		update(examName, status, description);
	}

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
		this(
				academicYear,
				classEntity,
				section,
				examType,
				examType == null ? null : examType.getName(),
				status,
				description);
		addSubject(subject, examDate, maxMarks, null);
	}

	public void update(LocalDate examDate, BigDecimal maxMarks, ExamScheduleStatus status, String description) {
		this.examDate = examDate;
		this.maxMarks = maxMarks;
		this.status = status == null ? ExamScheduleStatus.SCHEDULED : status;
		this.description = trimToNull(description);
	}

	public void update(String examName, ExamScheduleStatus status, String description) {
		this.examName = trimToNull(examName);
		this.status = status == null ? ExamScheduleStatus.SCHEDULED : status;
		this.description = trimToNull(description);
	}

	public ExamScheduleSubject addSubject(
			Subject subject,
			LocalDate examDate,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
		return addSubject(subject, examDate, null, null, null, maxMarks, passingMarks);
	}

	public ExamScheduleSubject addSubject(
			Subject subject,
			LocalDate examDate,
			java.time.LocalTime startTime,
			java.time.LocalTime endTime,
			String room,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
		ExamScheduleSubject scheduleSubject = new ExamScheduleSubject(
				this,
				subject,
				examDate,
				startTime,
				endTime,
				room,
				maxMarks,
				passingMarks);
		subjects.add(scheduleSubject);
		syncLegacySubjectFields();
		return scheduleSubject;
	}

	public Optional<ExamScheduleSubject> findSubject(java.util.UUID subjectId) {
		return subjects.stream()
				.filter(subject -> !subject.isDeleted())
				.filter(subject -> subject.getSubject().getId().equals(subjectId))
				.findFirst();
	}

	public List<ExamScheduleSubject> activeSubjects() {
		return subjects.stream()
				.filter(subject -> !subject.isDeleted())
				.sorted(Comparator.comparing(ExamScheduleSubject::getExamDate)
						.thenComparing(
								ExamScheduleSubject::getStartTime,
								Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(subject -> subject.getSubject().getName()))
				.toList();
	}

	public void syncLegacySubjectFields() {
		ExamScheduleSubject first = activeSubjects().stream().findFirst().orElse(null);
		subject = first == null ? null : first.getSubject();
		examDate = first == null ? null : first.getExamDate();
		maxMarks = first == null ? null : first.getMaxMarks();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
