package com.school.erp.modules.transport.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportFeeAssignedStatus;
import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student transport assignment details.")
public record StudentTransportAssignmentResponse(
		UUID id,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID academicYearId,
		String academicYear,
		UUID vehicleId,
		String vehicleNumber,
		String vehicleName,
		UUID routeId,
		String routeName,
		String routeCode,
		UUID pickupPointId,
		String pickupPointName,
		LocalTime pickupTime,
		LocalTime dropTime,
		UUID driverId,
		String driverName,
		String driverMobile,
		LocalDate assignmentDate,
		LocalDate endDate,
		TransportStatus status,
		TransportFeeAssignedStatus feeAssignedStatus,
		Instant createdAt,
		Instant updatedAt) {
}
