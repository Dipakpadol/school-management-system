package com.school.erp.modules.students.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Student photo metadata. File upload/storage is handled outside this payload.")
public record StudentPhotoRequest(
		@Size(max = 300) @Schema(example = "students/ADM-2026-0001/photo.jpg") String photoStorageKey,
		@Size(max = 500) @Schema(example = "https://files.school.test/students/ADM-2026-0001/photo.jpg") String photoUrl,
		@Size(max = 120) @Schema(example = "image/jpeg") String photoContentType,
		@Size(max = 180) @Schema(example = "photo.jpg") String photoFileName) {
}
