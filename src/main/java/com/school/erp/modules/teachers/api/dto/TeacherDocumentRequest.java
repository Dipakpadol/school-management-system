package com.school.erp.modules.teachers.api.dto;

import com.school.erp.modules.teachers.domain.TeacherDocumentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Teacher document metadata payload.")
public record TeacherDocumentRequest(
		@NotBlank @Size(max = 80) String documentType,
		@NotBlank @Size(max = 180) String fileName,
		@Size(max = 500) String fileUrl,
		@Size(max = 500) String filePath,
		TeacherDocumentStatus status) {
}
