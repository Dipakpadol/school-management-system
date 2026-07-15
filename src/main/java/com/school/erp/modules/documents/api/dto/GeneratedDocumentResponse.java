package com.school.erp.modules.documents.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.documents.domain.GeneratedDocumentStatus;
import com.school.erp.modules.documents.domain.GeneratedDocumentType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generated document metadata.")
public record GeneratedDocumentResponse(
		UUID id,
		UUID studentId,
		String admissionNumber,
		String studentName,
		UUID academicYearId,
		String academicYear,
		GeneratedDocumentType documentType,
		String documentNumber,
		LocalDate issueDate,
		String purpose,
		String remarks,
		String fileName,
		String contentType,
		long fileSize,
		GeneratedDocumentStatus status,
		UUID reprintOfDocumentId,
		Instant createdAt) {
}
