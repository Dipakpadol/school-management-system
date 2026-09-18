package com.school.erp.modules.staff.api.dto;

import com.school.erp.modules.staff.domain.StaffDocumentStatus;

import jakarta.validation.constraints.NotBlank;

public record StaffDocumentRequest(
		@NotBlank String documentType,
		@NotBlank String fileName,
		String fileUrl,
		String filePath,
		StaffDocumentStatus status) {
}
