package com.school.erp.modules.attendance.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
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
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "teacher_attendance")
@SQLRestriction("deleted = false")
public class TeacherAttendanceRecord extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AttendanceStatus status;

	@Column(length = 500)
	private String remarks;

	public TeacherAttendanceRecord(
			AcademicYear academicYear,
			Teacher teacher,
			LocalDate attendanceDate,
			AttendanceStatus status,
			String remarks) {
		this.academicYear = academicYear;
		this.teacher = teacher;
		this.attendanceDate = attendanceDate;
		update(status, remarks);
	}

	public void update(AttendanceStatus status, String remarks) {
		this.status = status == null ? AttendanceStatus.PRESENT : status;
		this.remarks = trimToNull(remarks);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
