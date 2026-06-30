package com.school.erp.modules.hostel.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.hostel.domain.HostelAllocationStatus;

public record HostelAllocationResponse(
		UUID id,
		UUID allocationId,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID academicYearId,
		String academicYear,
		String academicYearName,
		UUID hostelId,
		String hostelName,
		UUID roomId,
		String roomNumber,
		String roomType,
		UUID bedId,
		String bedNumber,
		LocalDate allocationDate,
		LocalDate vacateDate,
		HostelAllocationStatus status,
		String feeAssignedStatus,
		Instant createdAt,
		Instant updatedAt) {
}
