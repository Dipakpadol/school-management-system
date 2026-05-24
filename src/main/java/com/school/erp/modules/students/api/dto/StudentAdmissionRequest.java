package com.school.erp.modules.students.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Student admission payload with profile, guardians, initial class assignment, and documents.")
public record StudentAdmissionRequest(
		@NotBlank @Size(max = 40) @Schema(example = "ADM-2026-0001") String admissionNumber,
		@Valid @NotNull StudentProfileRequest profile,
		@Valid @NotEmpty List<ParentMappingRequest> parents,
		@Valid @NotNull ClassSectionAssignmentRequest classAssignment,
		@Valid List<StudentDocumentRequest> documents) {
}
