package com.school.erp.modules.hostel.api.dto;

import java.util.List;

import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;

public record HostelFeeAssignmentResponse(
		int createdAssignments,
		int skippedAssignments,
		List<StudentFeeAssignmentResponse> assignments) {
}
