package com.school.erp.modules.exams.api.dto;

import java.util.List;
import java.util.UUID;

public record MarksEntryResponse(
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		UUID examScheduleId,
		UUID subjectId,
		int totalRecords,
		List<ExamMarkResponse> records) {
}
