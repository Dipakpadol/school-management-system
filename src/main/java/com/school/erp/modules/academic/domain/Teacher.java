package com.school.erp.modules.academic.domain;

import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(length = 160)
	private String email;

	@Column(name = "phone_number", length = 30)
	private String phoneNumber;

	@Column(name = "user_account_id")
	private UUID userAccountId;

	@Column(nullable = false)
	private boolean active = true;

	public Teacher(String employeeNumber, String firstName, String lastName, String email, String phoneNumber) {
		this.employeeNumber = normalizeEmployeeNumber(employeeNumber);
		this.firstName = trim(firstName);
		this.lastName = trimToNull(lastName);
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
	}

	public String getDisplayName() {
		if (StringUtils.hasText(lastName)) {
			return firstName + " " + lastName;
		}
		return firstName;
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
