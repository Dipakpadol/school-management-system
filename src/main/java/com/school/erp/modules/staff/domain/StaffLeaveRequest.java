package com.school.erp.modules.staff.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.school.erp.common.domain.BaseEntity;

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
@Table(name = "staff_leave_requests")
@SQLRestriction("deleted = false")
public class StaffLeaveRequest extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "staff_id", nullable = false)
	private Staff staff;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "leave_type_id", nullable = false)
	private LeaveType leaveType;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(length = 500)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LeaveStatus status = LeaveStatus.PENDING;

	@Column(name = "requested_by", length = 120)
	private String requestedBy;

	@Column(name = "reviewed_by", length = 120)
	private String reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_comment", length = 500)
	private String reviewComment;

	public StaffLeaveRequest(
			Staff staff,
			LeaveType leaveType,
			LocalDate startDate,
			LocalDate endDate,
			String reason,
			String requestedBy) {
		this.staff = staff;
		this.leaveType = leaveType;
		this.startDate = startDate;
		this.endDate = endDate;
		this.reason = trimToNull(reason);
		this.requestedBy = trimToNull(requestedBy);
	}

	public long getDurationDays() {
		return ChronoUnit.DAYS.between(startDate, endDate) + 1;
	}

	public void approve(String reviewer, String comment) {
		this.status = LeaveStatus.APPROVED;
		this.reviewedBy = trimToNull(reviewer);
		this.reviewedAt = Instant.now();
		this.reviewComment = trimToNull(comment);
	}

	public void reject(String reviewer, String comment) {
		this.status = LeaveStatus.REJECTED;
		this.reviewedBy = trimToNull(reviewer);
		this.reviewedAt = Instant.now();
		this.reviewComment = trimToNull(comment);
	}

	public void cancel(String actor, String comment) {
		this.status = LeaveStatus.CANCELLED;
		this.reviewedBy = trimToNull(actor);
		this.reviewedAt = Instant.now();
		this.reviewComment = trimToNull(comment);
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
