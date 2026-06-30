package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record VacateHostelRequest(
		@NotNull LocalDate vacateDate) {
}
