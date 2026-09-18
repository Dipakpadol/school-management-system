package com.school.erp.modules.staff.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.attendance.domain.AttendanceStatus;

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
@Table(name = "staff_attendance")
@SQLRestriction("deleted = false")
public class StaffAttendance extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@Column(name = "attendance_date", nullable = false)
	private LocalDate attendanceDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AttendanceStatus status;

	@Column(length = 500)
	private String remarks;

	public StaffAttendance(Staff staff, LocalDate attendanceDate, AttendanceStatus status, String remarks) {
		this.staff = staff;
		this.attendanceDate = attendanceDate;
		update(status, remarks);
	}

	public void update(AttendanceStatus status, String remarks) {
		this.status = status == null ? AttendanceStatus.PRESENT : status;
		this.remarks = trimToNull(remarks);
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
