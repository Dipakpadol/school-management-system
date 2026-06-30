package com.school.erp.modules.transport.domain;

import java.time.LocalDate;

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
@Table(name = "transport_drivers")
@SQLRestriction("deleted = false")
public class TransportDriver extends BaseEntity {

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "middle_name", length = 80)
	private String middleName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(name = "mobile_number", nullable = false, length = 30)
	private String mobileNumber;

	@Column(name = "license_number", nullable = false, length = 80)
	private String licenseNumber;

	@Column(name = "license_expiry_date", nullable = false)
	private LocalDate licenseExpiryDate;

	@Column(length = 500)
	private String address;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransportStatus status = TransportStatus.ACTIVE;

	public TransportDriver(
			String firstName,
			String middleName,
			String lastName,
			String mobileNumber,
			String licenseNumber,
			LocalDate licenseExpiryDate,
			String address,
			TransportStatus status) {
		update(firstName, middleName, lastName, mobileNumber, licenseNumber, licenseExpiryDate, address, status);
	}

	public String getDisplayName() {
		StringBuilder name = new StringBuilder(firstName);
		if (StringUtils.hasText(middleName)) {
			name.append(" ").append(middleName);
		}
		if (StringUtils.hasText(lastName)) {
			name.append(" ").append(lastName);
		}
		return name.toString();
	}

	public boolean isActive() {
		return status == TransportStatus.ACTIVE;
	}

	public void update(
			String firstName,
			String middleName,
			String lastName,
			String mobileNumber,
			String licenseNumber,
			LocalDate licenseExpiryDate,
			String address,
			TransportStatus status) {
		this.firstName = trim(firstName);
		this.middleName = trimToNull(middleName);
		this.lastName = trimToNull(lastName);
		this.mobileNumber = trim(mobileNumber);
		this.licenseNumber = normalizeCode(licenseNumber);
		this.licenseExpiryDate = licenseExpiryDate;
		this.address = trimToNull(address);
		this.status = status == null ? TransportStatus.ACTIVE : status;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
