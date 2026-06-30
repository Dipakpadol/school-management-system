package com.school.erp.modules.transport.application;

import java.util.List;

import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.transport.api.dto.StudentTransportAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportDriverResponse;
import com.school.erp.modules.transport.api.dto.TransportPickupPointResponse;
import com.school.erp.modules.transport.api.dto.TransportRouteResponse;
import com.school.erp.modules.transport.api.dto.TransportStudentAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleDetailsResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleResponse;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportDriver;
import com.school.erp.modules.transport.domain.TransportPickupPoint;
import com.school.erp.modules.transport.domain.TransportRoute;
import com.school.erp.modules.transport.domain.TransportVehicle;

import org.springframework.stereotype.Component;

@Component
public class TransportMapper {

	public TransportDriverResponse toDriverResponse(TransportDriver driver) {
		if (driver == null) {
			return null;
		}
		return new TransportDriverResponse(
				driver.getId(),
				driver.getFirstName(),
				driver.getMiddleName(),
				driver.getLastName(),
				driver.getDisplayName(),
				driver.getMobileNumber(),
				driver.getLicenseNumber(),
				driver.getLicenseExpiryDate(),
				driver.getAddress(),
				driver.getStatus(),
				driver.getCreatedAt(),
				driver.getUpdatedAt());
	}

	public TransportVehicleResponse toVehicleResponse(TransportVehicle vehicle, int occupiedCount) {
		return new TransportVehicleResponse(
				vehicle.getId(),
				vehicle.getAcademicYear().getId(),
				vehicle.getAcademicYear().getName(),
				vehicle.getVehicleNumber(),
				vehicle.getVehicleName(),
				vehicle.getVehicleType(),
				vehicle.getCapacity(),
				occupiedCount,
				Math.max(vehicle.getCapacity() - occupiedCount, 0),
				vehicle.getDriver() == null ? null : vehicle.getDriver().getId(),
				vehicle.getDriver() == null ? null : vehicle.getDriver().getDisplayName(),
				vehicle.getDriver() == null ? null : vehicle.getDriver().getMobileNumber(),
				vehicle.getStatus(),
				vehicle.getCreatedAt(),
				vehicle.getUpdatedAt());
	}

	public TransportRouteResponse toRouteResponse(TransportRoute route) {
		return new TransportRouteResponse(
				route.getId(),
				route.getAcademicYear().getId(),
				route.getAcademicYear().getName(),
				route.getRouteName(),
				route.getRouteCode(),
				route.getStartLocation(),
				route.getEndLocation(),
				route.getVehicle() == null ? null : route.getVehicle().getId(),
				route.getVehicle() == null ? null : route.getVehicle().getVehicleNumber(),
				route.getVehicle() == null ? null : route.getVehicle().getVehicleName(),
				route.getStatus(),
				route.getCreatedAt(),
				route.getUpdatedAt());
	}

	public TransportPickupPointResponse toPickupPointResponse(TransportPickupPoint point) {
		return new TransportPickupPointResponse(
				point.getId(),
				point.getRoute().getId(),
				point.getRoute().getRouteName(),
				point.getPointName(),
				point.getPickupTime(),
				point.getDropTime(),
				point.getMonthlyFee(),
				point.getSequenceOrder(),
				point.getStatus(),
				point.getCreatedAt(),
				point.getUpdatedAt());
	}

	public StudentTransportAssignmentResponse toAssignmentResponse(StudentTransportAssignment assignment) {
		TransportDriver driver = assignment.getVehicle().getDriver();
		return new StudentTransportAssignmentResponse(
				assignment.getId(),
				assignment.getStudent().getId(),
				assignment.getStudent().getAdmissionNumber(),
				assignment.getStudent().getDisplayName(),
				assignment.getAcademicYear().getId(),
				assignment.getAcademicYear().getName(),
				assignment.getVehicle().getId(),
				assignment.getVehicle().getVehicleNumber(),
				assignment.getVehicle().getVehicleName(),
				assignment.getRoute().getId(),
				assignment.getRoute().getRouteName(),
				assignment.getRoute().getRouteCode(),
				assignment.getPickupPoint().getId(),
				assignment.getPickupPoint().getPointName(),
				assignment.getPickupPoint().getPickupTime(),
				assignment.getPickupPoint().getDropTime(),
				driver == null ? null : driver.getId(),
				driver == null ? null : driver.getDisplayName(),
				driver == null ? null : driver.getMobileNumber(),
				assignment.getAssignmentDate(),
				assignment.getEndDate(),
				assignment.getStatus(),
				assignment.getFeeAssignedStatus(),
				assignment.getCreatedAt(),
				assignment.getUpdatedAt());
	}

	public TransportStudentAssignmentResponse toStudentAssignmentSummary(StudentTransportAssignment assignment) {
		StudentClassAssignment classAssignment = assignment.getStudent().getClassAssignments().stream()
				.filter(existing -> !existing.isDeleted())
				.filter(StudentClassAssignment::isActive)
				.filter(existing -> existing.isForAcademicYear(assignment.getAcademicYear()))
				.findFirst()
				.orElseGet(() -> assignment.getStudent().getCurrentAssignment().orElse(null));
		return new TransportStudentAssignmentResponse(
				assignment.getId(),
				assignment.getStudent().getId(),
				assignment.getStudent().getAdmissionNumber(),
				assignment.getStudent().getDisplayName(),
				assignment.getAcademicYear().getId(),
				assignment.getAcademicYear().getName(),
				classAssignment == null || classAssignment.getClassEntity() == null ? null : classAssignment.getClassEntity().getId(),
				classAssignment == null || classAssignment.getSectionEntity() == null ? null : classAssignment.getSectionEntity().getId(),
				classAssignment == null ? null : classAssignment.getClassName(),
				classAssignment == null ? null : classAssignment.getSectionName(),
				assignment.getVehicle().getId(),
				assignment.getVehicle().getVehicleNumber(),
				assignment.getVehicle().getVehicleName(),
				assignment.getRoute().getId(),
				assignment.getRoute().getRouteName(),
				assignment.getPickupPoint().getId(),
				assignment.getPickupPoint().getPointName(),
				assignment.getAssignmentDate(),
				assignment.getEndDate(),
				assignment.getStatus(),
				assignment.getFeeAssignedStatus());
	}

	public TransportVehicleDetailsResponse toVehicleDetails(
			TransportVehicle vehicle,
			int occupiedCount,
			List<TransportRouteResponse> routes,
			List<TransportPickupPointResponse> pickupPoints,
			List<TransportStudentAssignmentResponse> students) {
		return new TransportVehicleDetailsResponse(
				toVehicleResponse(vehicle, occupiedCount),
				toDriverResponse(vehicle.getDriver()),
				routes,
				pickupPoints,
				students);
	}
}
