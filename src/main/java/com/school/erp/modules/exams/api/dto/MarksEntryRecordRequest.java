package com.school.erp.modules.exams.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarksEntryRecordRequest(
		@NotNull UUID studentId,
		@NotNull @DecimalMin(value = "0.00") BigDecimal marksObtained,
		@DecimalMin(value = "0.01") BigDecimal maxMarks,
		@Size(max = 500) String remarks) {
}
