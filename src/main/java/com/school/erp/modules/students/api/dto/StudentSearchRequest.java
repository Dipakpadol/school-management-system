package com.school.erp.modules.students.api.dto;

import java.time.LocalDate;

import com.school.erp.modules.students.domain.StudentStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Student search and filter criteria.")
public record StudentSearchRequest(
		@Size(max = 120) @Schema(description = "Searches admission number, name, email, and phone.", example = "aarav")
		String query,
		@Schema(example = "ACTIVE") StudentStatus status,
		@Size(max = 40) @Schema(example = "ADM-2026-0001") String admissionNumber,
		@Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-04-01") LocalDate admittedFrom,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Schema(example = "2026-04-30") LocalDate admittedTo) {
}
