package com.school.erp.modules.portal.api.dto;

import com.school.erp.common.api.PageResponse;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.exams.api.dto.StudentExamResultsResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.transport.api.dto.StudentTransportAssignmentResponse;

public record PortalStudentDashboardResponse(
		PortalStudentProfileResponse profile,
		StudentAttendanceHistoryResponse attendance,
		StudentFeeSummaryResponse fees,
		StudentExamResultsResponse exams,
		PortalLibrarySummaryResponse library,
		HostelAllocationResponse hostel,
		StudentTransportAssignmentResponse transport,
		PageResponse<CommunicationResponse> communications) {
}
