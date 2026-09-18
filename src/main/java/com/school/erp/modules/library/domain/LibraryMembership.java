package com.school.erp.modules.library.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "library_memberships")
@SQLRestriction("deleted = false")
public class LibraryMembership extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "member_type", nullable = false, length = 30)
	private LibraryMemberType memberType;

	@Column(name = "student_id")
	private UUID studentId;

	@Column(name = "teacher_id")
	private UUID teacherId;

	@Column(name = "staff_id")
	private UUID staffId;

	@Column(name = "membership_number", nullable = false, length = 60)
	private String membershipNumber;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	@Column(nullable = false)
	private boolean active = true;

	@Column(length = 500)
	private String notes;

	public LibraryMembership(
			LibraryMemberType memberType,
			UUID studentId,
			UUID teacherId,
			UUID staffId,
			String membershipNumber,
			LocalDate startDate,
			LocalDate expiryDate,
			boolean active,
			String notes) {
		update(memberType, studentId, teacherId, staffId, membershipNumber, startDate, expiryDate, active, notes);
	}

	public void update(
			LibraryMemberType memberType,
			UUID studentId,
			UUID teacherId,
			UUID staffId,
			String membershipNumber,
			LocalDate startDate,
			LocalDate expiryDate,
			boolean active,
			String notes) {
		this.memberType = memberType;
		this.studentId = studentId;
		this.teacherId = teacherId;
		this.staffId = staffId;
		this.membershipNumber = normalizeMembershipNumber(membershipNumber);
		this.startDate = startDate;
		this.expiryDate = expiryDate;
		this.active = active;
		this.notes = trimToNull(notes);
	}

	public void deactivate() {
		active = false;
	}

	public UUID memberId() {
		return switch (memberType) {
			case STUDENT -> studentId;
			case TEACHER -> teacherId;
			case STAFF -> staffId;
		};
	}

	public boolean isCurrent(LocalDate date) {
		return active
				&& (startDate == null || !startDate.isAfter(date))
				&& (expiryDate == null || !expiryDate.isBefore(date));
	}

	private String normalizeMembershipNumber(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
