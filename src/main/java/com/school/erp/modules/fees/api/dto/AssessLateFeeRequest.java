package com.school.erp.modules.fees.api.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Late fee assessment request.")
public record AssessLateFeeRequest(
		@Schema(description = "Defaults to current date when omitted.", example = "2026-07-01") LocalDate asOf) {
}
