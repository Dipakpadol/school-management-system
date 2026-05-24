package com.school.erp.modules.students.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.students.domain.DocumentType;
import com.school.erp.modules.students.domain.DocumentVerificationStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Student document metadata response.")
public record StudentDocumentResponse(
		UUID id,
		DocumentType documentType,
		String documentNumber,
		String fileName,
		String contentType,
		Long fileSize,
		String storageKey,
		String fileUrl,
		DocumentVerificationStatus verificationStatus,
		String remarks,
		Instant verifiedAt,
		String verifiedBy,
		Instant createdAt) {
}
