package com.school.erp.modules.documents.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.documents.domain.GeneratedDocumentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Certificate generation request.")
public record CertificateGenerationRequest(
		@NotNull UUID studentId,
		@NotNull UUID academicYearId,
		@NotNull GeneratedDocumentType documentType,
		LocalDate issueDate,
		@Size(max = 300) String purpose,
		@Size(max = 500) String remarks,
		LocalDate dateOfAdmission,
		LocalDate dateOfLeaving,
		@Size(max = 120) String lastClassStudied,
		@Size(max = 300) String reasonForLeaving,
		@Size(max = 120) String conduct,
		@Size(max = 120) String progress,
		@Size(max = 80) String nationality,
		@Size(max = 120) String religionOrCaste) {
}
