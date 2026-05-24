package com.school.erp.modules.students.api.dto;

import com.school.erp.modules.students.domain.ParentRelation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Relationship between a student and a parent or guardian.")
public record ParentMappingRequest(
		@NotNull @Schema(example = "FATHER") ParentRelation relationType,
		@Schema(example = "true") boolean primaryContact,
		@Schema(example = "true") boolean emergencyContact,
		@Schema(example = "true") boolean pickupAllowed,
		@Valid @NotNull ParentGuardianRequest parent) {
}
