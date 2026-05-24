package com.school.erp.modules.students.api.dto;

import com.school.erp.modules.students.domain.StudentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Student status update.")
public record StudentStatusUpdateRequest(
		@NotNull @Schema(example = "ACTIVE") StudentStatus status) {
}
