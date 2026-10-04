package com.school.erp.modules.portal.api.dto;

import com.school.erp.common.api.PageResponse;
import com.school.erp.modules.library.api.dto.LibraryFineResponse;
import com.school.erp.modules.library.api.dto.LibraryLoanResponse;

public record PortalLibrarySummaryResponse(
		PortalLibraryMembershipResponse membership,
		PageResponse<LibraryLoanResponse> loans,
		PageResponse<LibraryFineResponse> fines) {
}
