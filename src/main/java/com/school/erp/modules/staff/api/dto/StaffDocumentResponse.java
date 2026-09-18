package com.school.erp.modules.staff.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.staff.domain.StaffDocumentStatus;

public record StaffDocumentResponse(
		UUID id,
		UUID staffId,
		String documentType,
		String fileName,
		String fileUrl,
		String filePath,
		Instant uploadedAt,
		String uploadedBy,
		StaffDocumentStatus status) {
}
