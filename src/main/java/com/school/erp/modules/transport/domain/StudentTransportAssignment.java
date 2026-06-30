package com.school.erp.modules.transport.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.students.domain.Student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_transport_assignments")
@SQLRestriction("deleted = false")
public class StudentTransportAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private TransportVehicle vehicle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private TransportRoute route;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pickup_point_id", nullable = false)
	private TransportPickupPoint pickupPoint;

	@Column(name = "assignment_date", nullable = false)
	private LocalDate assignmentDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransportStatus status = TransportStatus.ASSIGNED;

	@Enumerated(EnumType.STRING)
	@Column(name = "fee_assigned_status", nullable = false, length = 30)
	private TransportFeeAssignedStatus feeAssignedStatus = TransportFeeAssignedStatus.NOT_ASSIGNED;

	public StudentTransportAssignment(
			Student student,
			AcademicYear academicYear,
			TransportVehicle vehicle,
			TransportRoute route,
			TransportPickupPoint pickupPoint,
			LocalDate assignmentDate) {
		this.student = student;
		this.academicYear = academicYear;
		this.vehicle = vehicle;
		this.route = route;
		this.pickupPoint = pickupPoint;
		this.assignmentDate = assignmentDate;
	}

	public boolean isAssigned() {
		return status == TransportStatus.ASSIGNED;
	}

	public void markFeeAssigned(boolean assigned) {
		this.feeAssignedStatus = assigned
				? TransportFeeAssignedStatus.ASSIGNED
				: TransportFeeAssignedStatus.NOT_ASSIGNED;
	}

	public void transfer(LocalDate transferDate) {
		this.endDate = transferDate;
		this.status = TransportStatus.TRANSFERRED;
	}

	public void remove(LocalDate removeDate) {
		this.endDate = removeDate;
		this.status = TransportStatus.REMOVED;
	}
}
