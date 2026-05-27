package com.school.erp.modules.students.domain;

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
@Table(name = "parents")
@SQLRestriction("deleted = false")
public class ParentGuardian extends BaseEntity {

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(length = 160)
	private String email;

	@Column(name = "phone_number", nullable = false, length = 30)
	private String phoneNumber;

	@Column(name = "alternate_phone_number", length = 30)
	private String alternatePhoneNumber;

	@Column(length = 120)
	private String occupation;

	@Column(name = "address_line_1", length = 160)
	private String addressLine1;

	@Column(name = "address_line_2", length = 160)
	private String addressLine2;

	@Column(length = 80)
	private String city;

	@Column(length = 80)
	private String state;

	@Column(name = "postal_code", length = 20)
	private String postalCode;

	@Column(length = 80)
	private String country;

	@Column(name = "user_account_id")
	private UUID userAccountId;

	public ParentGuardian(String firstName, String lastName, String email, String phoneNumber) {
		this.firstName = firstName;
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

	public void updateProfile(
			String firstName,
			String lastName,
			String email,
			String phoneNumber,
			String alternatePhoneNumber,
			String occupation,
			String addressLine1,
			String addressLine2,
			String city,
			String state,
			String postalCode,
			String country,
			UUID userAccountId) {
		this.firstName = firstName;
		this.lastName = trimToNull(lastName);
		this.email = normalizeEmail(email);
		this.phoneNumber = trimToNull(phoneNumber);
		this.alternatePhoneNumber = trimToNull(alternatePhoneNumber);
		this.occupation = trimToNull(occupation);
		this.addressLine1 = trimToNull(addressLine1);
		this.addressLine2 = trimToNull(addressLine2);
		this.city = trimToNull(city);
		this.state = trimToNull(state);
		this.postalCode = trimToNull(postalCode);
		this.country = trimToNull(country);
		this.userAccountId = userAccountId;
	}

	private String normalizeEmail(String value) {
		return value == null ? null : trimToNull(value.toLowerCase());
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
