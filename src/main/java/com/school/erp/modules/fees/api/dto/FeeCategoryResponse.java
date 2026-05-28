package com.school.erp.modules.fees.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee category response.")
public record FeeCategoryResponse(
		UUID id,
		String code,
		String name,
		String description,
		boolean active,
		int sortOrder,
		@JsonProperty("isMandatory") boolean mandatory,
		Instant createdAt,
		Instant updatedAt) {
}
