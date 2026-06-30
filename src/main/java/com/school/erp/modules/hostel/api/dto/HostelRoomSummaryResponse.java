package com.school.erp.modules.hostel.api.dto;

import java.util.List;
import java.util.UUID;

public record HostelRoomSummaryResponse(
		UUID id,
		UUID hostelId,
		String hostelName,
		String roomNumber,
		String roomType,
		int capacity,
		int occupiedCount,
		int availableBeds,
		boolean bedConceptEnabled,
		boolean active,
		List<HostelBedResponse> beds) {
}
