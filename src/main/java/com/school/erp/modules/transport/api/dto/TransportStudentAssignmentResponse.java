package com.school.erp.modules.transport.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportFeeAssignedStatus;
import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student assigned to a transport vehicle or route.")
public record TransportStudentAssignmentResponse(
		UUID assignmentId,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		UUID sectionId,
		String className,
		String sectionName,
		UUID vehicleId,
		String vehicleNumber,
		String vehicleName,
		UUID routeId,
		String routeName,
		UUID pickupPointId,
		String pickupPointName,
		LocalDate assignmentDate,
		LocalDate endDate,
		TransportStatus status,
		TransportFeeAssignedStatus feeAssignedStatus) {
}
