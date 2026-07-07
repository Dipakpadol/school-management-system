package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

@Schema(description = "Defaulter report filters.")
public record DefaulterSearchRequest(
		@Schema(description = "Defaults to current date when omitted.", example = "2026-07-01") LocalDate asOf,
		@Schema(description = "Academic year UUID for hierarchy-based filtering.") UUID academicYearId,
		@Schema(description = "Class UUID for hierarchy-based filtering.") UUID classId,
		@Schema(description = "Section UUID for hierarchy-based filtering.") UUID sectionId,
		@Size(max = 20) @Schema(example = "2026-2027") String academicYear,
		@Size(max = 80) @Schema(example = "Class 6") String className,
		@Size(max = 80) @Schema(example = "A") String sectionName,
		@Size(max = 120) @Schema(example = "aarav") String studentName,
		@DecimalMin("0.01") @Digits(integer = 10, fraction = 2) @Schema(example = "100.00") BigDecimal minimumBalance) {
}
