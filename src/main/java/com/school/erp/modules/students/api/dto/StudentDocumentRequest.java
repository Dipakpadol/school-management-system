package com.school.erp.modules.students.api.dto;

import com.school.erp.modules.students.domain.DocumentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Student document metadata. File upload/storage is handled outside this payload.")
public record StudentDocumentRequest(
		@NotNull @Schema(example = "BIRTH_CERTIFICATE") DocumentType documentType,
		@Size(max = 80) @Schema(example = "BC-2026-00017") String documentNumber,
		@NotBlank @Size(max = 180) @Schema(example = "birth-certificate.pdf") String fileName,
		@Size(max = 120) @Schema(example = "application/pdf") String contentType,
		@PositiveOrZero @Schema(example = "240128") Long fileSize,
		@Size(max = 300) @Schema(example = "students/ADM-2026-0001/birth-certificate.pdf") String storageKey,
		@Size(max = 500) @Schema(example = "https://files.school.test/students/ADM-2026-0001/birth-certificate.pdf")
		String fileUrl,
		@Size(max = 500) @Schema(example = "Original verified during admission") String remarks) {
}
