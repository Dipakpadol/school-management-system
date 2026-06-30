package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeRoomRequest(
		@NotNull UUID roomId,
		UUID bedId,
		@Size(max = 60) String bedNumber,
		@NotNull LocalDate allocationDate,
		boolean hostelFeeApplicable) {
}
