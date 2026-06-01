package com.school.erp.modules.exams.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record MarksEntryRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID classId,
		@NotNull UUID sectionId,
		@NotNull UUID examScheduleId,
		@NotNull UUID subjectId,
		@Valid @NotEmpty List<MarksEntryRecordRequest> records) {
}
