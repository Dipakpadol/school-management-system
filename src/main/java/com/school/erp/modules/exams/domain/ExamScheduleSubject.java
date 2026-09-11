package com.school.erp.modules.exams.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.Subject;

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
@Table(name = "exam_schedule_subjects")
@SQLRestriction("deleted = false")
public class ExamScheduleSubject extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "exam_schedule_id", nullable = false)
	private ExamSchedule examSchedule;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id", nullable = false)
	private Subject subject;

	@Column(name = "exam_date", nullable = false)
	private LocalDate examDate;

	@Column(name = "start_time")
	private LocalTime startTime;

	@Column(name = "end_time")
	private LocalTime endTime;

	@Column(length = 120)
	private String room;

	@Column(name = "max_marks", nullable = false, precision = 6, scale = 2)
	private BigDecimal maxMarks;

	@Column(name = "passing_marks", precision = 6, scale = 2)
	private BigDecimal passingMarks;

	public ExamScheduleSubject(
			ExamSchedule examSchedule,
			Subject subject,
			LocalDate examDate,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
		this(examSchedule, subject, examDate, null, null, null, maxMarks, passingMarks);
	}

	public ExamScheduleSubject(
			ExamSchedule examSchedule,
			Subject subject,
			LocalDate examDate,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
		this.examSchedule = examSchedule;
		this.subject = subject;
		update(examDate, startTime, endTime, room, maxMarks, passingMarks);
	}

	public void update(LocalDate examDate, BigDecimal maxMarks, BigDecimal passingMarks) {
		update(examDate, null, null, null, maxMarks, passingMarks);
	}

	public void update(
			LocalDate examDate,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			BigDecimal maxMarks,
			BigDecimal passingMarks) {
		this.examDate = examDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.room = trimToNull(room);
		this.maxMarks = maxMarks;
		this.passingMarks = passingMarks;
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
