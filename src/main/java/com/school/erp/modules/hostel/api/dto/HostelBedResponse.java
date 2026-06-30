package com.school.erp.modules.hostel.api.dto;

import java.util.UUID;

public record HostelBedResponse(
		UUID id,
		String bedNumber,
		boolean active,
		boolean occupied) {
}
