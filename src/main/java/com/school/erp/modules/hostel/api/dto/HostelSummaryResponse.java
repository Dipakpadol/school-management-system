package com.school.erp.modules.hostel.api.dto;

import java.util.UUID;

public record HostelSummaryResponse(
		UUID id,
		String code,
		String name,
		String address,
		boolean active) {
}
