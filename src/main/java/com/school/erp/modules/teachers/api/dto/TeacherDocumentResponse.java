package com.school.erp.modules.teachers.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.teachers.domain.TeacherDocumentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher document metadata response.")
public record TeacherDocumentResponse(
		UUID id,
		UUID teacherId,
		String documentType,
		String fileName,
		String fileUrl,
		String filePath,
		Instant uploadedAt,
		TeacherDocumentStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
