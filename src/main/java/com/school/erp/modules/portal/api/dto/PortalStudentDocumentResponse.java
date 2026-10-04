package com.school.erp.modules.portal.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.students.domain.DocumentType;
import com.school.erp.modules.students.domain.DocumentVerificationStatus;

public record PortalStudentDocumentResponse(
		UUID id,
		DocumentType documentType,
		String documentNumber,
		String fileName,
		String contentType,
		Long fileSize,
		String fileUrl,
		DocumentVerificationStatus verificationStatus,
		String remarks,
		Instant verifiedAt,
		Instant createdAt) {
}
