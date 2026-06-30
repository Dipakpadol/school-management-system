package com.school.erp.modules.transport.domain;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;

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
@Table(name = "transport_vehicles")
@SQLRestriction("deleted = false")
public class TransportVehicle extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@Column(name = "vehicle_number", nullable = false, length = 60)
	private String vehicleNumber;

	@Column(name = "vehicle_name", nullable = false, length = 160)
	private String vehicleName;

	@Column(name = "vehicle_type", nullable = false, length = 80)
	private String vehicleType;

	@Column(nullable = false)
	private int capacity;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id")
	private TransportDriver driver;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransportStatus status = TransportStatus.ACTIVE;

	public TransportVehicle(
			AcademicYear academicYear,
			String vehicleNumber,
			String vehicleName,
			String vehicleType,
			int capacity,
			TransportDriver driver,
			TransportStatus status) {
		update(academicYear, vehicleNumber, vehicleName, vehicleType, capacity, driver, status);
	}

	public boolean isActive() {
		return status == TransportStatus.ACTIVE;
	}

	public void update(
			AcademicYear academicYear,
			String vehicleNumber,
			String vehicleName,
			String vehicleType,
			int capacity,
			TransportDriver driver,
			TransportStatus status) {
		this.academicYear = academicYear;
		this.vehicleNumber = normalizeCode(vehicleNumber);
		this.vehicleName = trim(vehicleName);
		this.vehicleType = normalizeCode(vehicleType);
		this.capacity = capacity;
		this.driver = driver;
		this.status = status == null ? TransportStatus.ACTIVE : status;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
