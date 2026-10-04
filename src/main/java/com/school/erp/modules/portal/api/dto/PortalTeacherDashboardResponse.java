package com.school.erp.modules.portal.api.dto;

import java.util.List;

import com.school.erp.common.api.PageResponse;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.teachers.api.dto.TeacherAssignmentResponse;
import com.school.erp.modules.teachers.api.dto.TeacherResponse;

public record PortalTeacherDashboardResponse(
		TeacherResponse profile,
		List<TeacherAssignmentResponse> assignments,
		List<PortalTeacherScopeResponse> scopes,
		PageResponse<CommunicationResponse> communications) {
}
