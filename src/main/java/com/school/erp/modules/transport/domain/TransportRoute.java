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
@Table(name = "transport_routes")
@SQLRestriction("deleted = false")
public class TransportRoute extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@Column(name = "route_name", nullable = false, length = 160)
	private String routeName;

	@Column(name = "route_code", nullable = false, length = 60)
	private String routeCode;

	@Column(name = "start_location", nullable = false, length = 160)
	private String startLocation;

	@Column(name = "end_location", nullable = false, length = 160)
	private String endLocation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id")
	private TransportVehicle vehicle;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransportStatus status = TransportStatus.ACTIVE;

	public TransportRoute(
			AcademicYear academicYear,
			String routeName,
			String routeCode,
			String startLocation,
			String endLocation,
			TransportVehicle vehicle,
			TransportStatus status) {
		update(academicYear, routeName, routeCode, startLocation, endLocation, vehicle, status);
	}

	public boolean isActive() {
		return status == TransportStatus.ACTIVE;
	}

	public void update(
			AcademicYear academicYear,
			String routeName,
			String routeCode,
			String startLocation,
			String endLocation,
			TransportVehicle vehicle,
			TransportStatus status) {
		this.academicYear = academicYear;
		this.routeName = trim(routeName);
		this.routeCode = normalizeCode(routeCode);
		this.startLocation = trim(startLocation);
		this.endLocation = trim(endLocation);
		this.vehicle = vehicle;
		this.status = status == null ? TransportStatus.ACTIVE : status;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
