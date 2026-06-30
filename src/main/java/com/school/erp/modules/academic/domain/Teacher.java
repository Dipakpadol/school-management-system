package com.school.erp.modules.academic.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.students.domain.Gender;

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
@Table(name = "teachers")
@SQLRestriction("deleted = false")
public class Teacher extends BaseEntity {

	@Column(name = "employee_number", nullable = false, length = 40)
	private String employeeNumber;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "middle_name", length = 80)
	private String middleName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private Gender gender;

	@Column(name = "date_of_birth")
	private LocalDate dateOfBirth;

	@Column(length = 160)
	private String email;

	@Column(name = "phone_number", length = 30)
	private String phoneNumber;

	@Column(length = 160)
	private String qualification;

	@Column(name = "experience_years")
	private Integer experienceYears;

	@Column(name = "joining_date")
	private LocalDate joiningDate;

	@Column(name = "user_account_id")
	private UUID userAccountId;

	@Column(nullable = false)
	private boolean active = true;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TeacherStatus status = TeacherStatus.ACTIVE;

	public Teacher(String employeeNumber, String firstName, String lastName, String email, String phoneNumber) {
		this.employeeNumber = normalizeEmployeeNumber(employeeNumber);
		this.firstName = trim(firstName);
		this.lastName = trimToNull(lastName);
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
	}

	public String getDisplayName() {
		StringBuilder displayName = new StringBuilder(firstName);
		if (StringUtils.hasText(middleName)) {
			displayName.append(" ").append(middleName);
		}
		if (StringUtils.hasText(lastName)) {
			displayName.append(" ").append(lastName);
		}
		return displayName.toString();
	}

	public String getMobileNumber() {
		return phoneNumber;
	}

	public boolean isActive() {
		return active && status == TeacherStatus.ACTIVE;
	}

	public void update(
			String employeeNumber,
			String firstName,
			String lastName,
			String email,
			String phoneNumber,
			UUID userAccountId,
			boolean active) {
		this.employeeNumber = normalizeEmployeeNumber(employeeNumber);
		this.firstName = trim(firstName);
		this.lastName = trimToNull(lastName);
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
		this.userAccountId = userAccountId;
		this.active = active;
		this.status = active ? TeacherStatus.ACTIVE : TeacherStatus.INACTIVE;
	}

	public void updateProfile(
			String employeeNumber,
			String firstName,
			String middleName,
			String lastName,
			Gender gender,
			LocalDate dateOfBirth,
			String mobileNumber,
			String email,
			String qualification,
			Integer experienceYears,
			LocalDate joiningDate,
			TeacherStatus status,
			UUID userAccountId) {
		this.employeeNumber = normalizeEmployeeNumber(employeeNumber);
		this.firstName = trim(firstName);
		this.middleName = trimToNull(middleName);
		this.lastName = trimToNull(lastName);
		this.gender = gender;
		this.dateOfBirth = dateOfBirth;
		this.phoneNumber = trimToNull(mobileNumber);
		this.email = normalizeEmail(email);
		this.qualification = trimToNull(qualification);
		this.experienceYears = experienceYears;
		this.joiningDate = joiningDate;
		this.status = status == null ? TeacherStatus.ACTIVE : status;
		this.active = this.status == TeacherStatus.ACTIVE;
		this.userAccountId = userAccountId;
	}

	private String normalizeEmployeeNumber(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String normalizeEmail(String value) {
		return value == null ? null : trimToNull(value.toLowerCase());
	}

	private String trim(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
