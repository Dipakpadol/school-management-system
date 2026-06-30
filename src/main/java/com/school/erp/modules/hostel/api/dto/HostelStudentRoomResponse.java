package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.hostel.domain.HostelAllocationStatus;

public record HostelStudentRoomResponse(
		UUID allocationId,
		UUID studentId,
		String studentName,
		String admissionNumber,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		UUID sectionId,
		String className,
		String divisionName,
		String bedNumber,
		LocalDate allocationDate,
		LocalDate vacateDate,
		HostelAllocationStatus status) {
}
