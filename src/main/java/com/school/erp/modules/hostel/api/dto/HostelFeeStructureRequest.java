package com.school.erp.modules.hostel.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeStructureStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HostelFeeStructureRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID hostelId,
		UUID roomId,
		@Size(max = 80) String roomType,
		@NotNull UUID feeCategoryId,
		@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
		@NotNull LocalDate dueDate,
		boolean installmentAllowed,
		@Min(1) int numberOfInstallments,
		@Schema(example = "ACTIVE") FeeStructureStatus status,
		@Size(max = 140) String name,
		@Size(max = 500) String description) {
}
