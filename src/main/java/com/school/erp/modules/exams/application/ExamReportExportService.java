package com.school.erp.modules.exams.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.exams.api.dto.ExamMarkResponse;
import com.school.erp.modules.exams.api.dto.ExamScheduleResponse;
import com.school.erp.modules.exams.api.dto.MarksEntryResponse;
import com.school.erp.modules.exams.api.dto.StudentResultResponse;
import com.school.erp.modules.exams.api.dto.SubjectResultResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExamReportExportService {

	public static final List<String> SCHEDULE_HEADERS = List.of(
			"academicYear",
			"className",
			"sectionName",
			"examType",
			"examName",
			"subject",
			"examDate",
			"startTime",
			"endTime",
			"room",
			"maxMarks",
			"passingMarks",
			"status");
	public static final List<String> MARKS_HEADERS = List.of(
			"academicYearId",
			"classId",
			"sectionId",
			"examScheduleId",
			"subject",
			"admissionNumber",
			"rollNumber",
			"studentName",
			"marksObtained",
			"maxMarks",
			"passingMarks",
			"remarks");
	public static final List<String> RESULT_HEADERS = List.of(
			"studentId",
			"admissionNumber",
			"studentName",
			"academicYearId",
			"classId",
			"sectionId",
			"subject",
			"marksObtained",
			"maxMarks",
			"subjectGrade",
			"subjectPassed",
			"totalMarks",
			"resultMaxMarks",
			"percentage",
			"grade",
			"passed",
			"rank");
	public static final List<String> PASS_FAIL_HEADERS = List.of(
			"totalStudents",
			"passed",
			"failed",
			"passPercentage");
	public static final List<String> GRADE_HEADERS = List.of(
			"grade",
			"students");

	private final ExamService examService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	@Transactional(readOnly = true)
	public byte[] scheduleReport(
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId) {
		return export(
				format,
				"Exam Schedule Report",
				"exam-schedule-report",
				SCHEDULE_HEADERS,
				scheduleRows(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId),
				filters(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> scheduleRows(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId) {
		return examService.getScheduleReport(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId)
				.stream()
				.flatMap(schedule -> schedule.subjects().stream()
						.filter(subject -> subjectId == null || subject.subjectId().equals(subjectId))
						.map(subject -> {
							Map<String, Object> row = new LinkedHashMap<>();
							row.put("academicYear", schedule.academicYearName());
							row.put("className", schedule.className());
							row.put("sectionName", schedule.sectionName());
							row.put("examType", schedule.examTypeName());
							row.put("examName", schedule.examName());
							row.put("subject", subject.subjectName());
							row.put("examDate", subject.examDate());
							row.put("startTime", subject.startTime());
							row.put("endTime", subject.endTime());
							row.put("room", subject.room());
							row.put("maxMarks", subject.maxMarks());
							row.put("passingMarks", subject.passingMarks());
							row.put("status", schedule.status());
							return row;
						}))
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] marksReport(
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return export(
				format,
				"Exam Marks Report",
				"exam-marks-report",
				MARKS_HEADERS,
				marksRows(academicYearId, classId, sectionId, examScheduleId, subjectId, studentId),
				filters(academicYearId, classId, sectionId, null, examScheduleId, subjectId, studentId));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> marksRows(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		MarksEntryResponse marks = examService.getMarks(academicYearId, classId, sectionId, examScheduleId, subjectId);
		return marks.records().stream()
				.filter(mark -> studentId == null || mark.student().studentId().equals(studentId))
				.map(mark -> markRow(marks, mark))
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] resultReport(
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return export(
				format,
				"Exam Result Report",
				"exam-result-report",
				RESULT_HEADERS,
				resultRows(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId),
				filters(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> resultRows(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return resultResponses(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId)
				.stream()
				.flatMap(result -> result.subjects().stream()
						.filter(subject -> subjectId == null || subject.subjectId().equals(subjectId))
						.map(subject -> resultRow(result, subject)))
				.toList();
	}

	@Transactional(readOnly = true)
	public byte[] passFailSummaryReport(
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return export(
				format,
				"Exam Pass Fail Summary",
				"exam-pass-fail-summary",
				PASS_FAIL_HEADERS,
				passFailRows(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId),
				filters(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> passFailRows(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		List<StudentResultResponse> results = resultResponses(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId);
		long passed = results.stream().filter(StudentResultResponse::passed).count();
		long failed = results.size() - passed;
		BigDecimal passPercentage = results.isEmpty()
				? BigDecimal.ZERO
				: BigDecimal.valueOf(passed)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(results.size()), 2, RoundingMode.HALF_UP);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("totalStudents", results.size());
		row.put("passed", passed);
		row.put("failed", failed);
		row.put("passPercentage", passPercentage);
		return List.of(row);
	}

	@Transactional(readOnly = true)
	public byte[] gradeSummaryReport(
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return export(
				format,
				"Exam Grade Summary",
				"exam-grade-summary",
				GRADE_HEADERS,
				gradeRows(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId),
				filters(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> gradeRows(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		Map<String, Long> counts = resultResponses(academicYearId, classId, sectionId, examTypeId, examScheduleId, subjectId, studentId)
				.stream()
				.collect(Collectors.groupingBy(
						result -> result.grade() == null ? "UNASSIGNED" : result.grade(),
						LinkedHashMap::new,
						Collectors.counting()));
		return List.of("A+", "A", "B", "C", "D", "F", "UNASSIGNED").stream()
				.filter(counts::containsKey)
				.map(grade -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("grade", grade);
					row.put("students", counts.get(grade));
					return row;
				})
				.toList();
	}

	private List<StudentResultResponse> resultResponses(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		return examService.getResultReport(
				academicYearId,
				classId,
				sectionId,
				examTypeId,
				examScheduleId,
				subjectId,
				studentId);
	}

	private Map<String, Object> markRow(MarksEntryResponse marks, ExamMarkResponse mark) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("academicYearId", marks.academicYearId());
		row.put("classId", marks.classId());
		row.put("sectionId", marks.sectionId());
		row.put("examScheduleId", marks.examScheduleId());
		row.put("subject", mark.subjectName());
		row.put("admissionNumber", mark.student().admissionNumber());
		row.put("rollNumber", mark.student().rollNumber());
		row.put("studentName", mark.student().displayName());
		row.put("marksObtained", mark.marksObtained());
		row.put("maxMarks", mark.maxMarks());
		row.put("passingMarks", marks.passingMarks());
		row.put("remarks", mark.remarks());
		return row;
	}

	private Map<String, Object> resultRow(StudentResultResponse result, SubjectResultResponse subject) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("studentId", result.studentId());
		row.put("admissionNumber", result.admissionNumber());
		row.put("studentName", result.studentName());
		row.put("academicYearId", result.academicYearId());
		row.put("classId", result.classId());
		row.put("sectionId", result.sectionId());
		row.put("subject", subject.subjectName());
		row.put("marksObtained", subject.marksObtained());
		row.put("maxMarks", subject.maxMarks());
		row.put("subjectGrade", subject.grade());
		row.put("subjectPassed", subject.passed());
		row.put("totalMarks", result.totalMarks());
		row.put("resultMaxMarks", result.maxMarks());
		row.put("percentage", result.percentage());
		row.put("grade", result.grade());
		row.put("passed", result.passed());
		row.put("rank", result.rank());
		return row;
	}

	private byte[] export(
			String format,
			String title,
			String sheetName,
			List<String> headers,
			List<Map<String, Object>> rows,
			Map<String, Object> filters) {
		return switch (normalizeFormat(format)) {
			case "CSV" -> csvExportService.export(headers, rows);
			case "PDF" -> pdfExportService.exportTable(title, filters, headers, rows);
			case "EXCEL" -> excelExportService.export(sheetName, headers, rows);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, title + " does not support " + format + " export.");
		};
	}

	private Map<String, Object> filters(
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Academic Year ID", academicYearId);
		filters.put("Class ID", classId);
		filters.put("Section ID", sectionId);
		filters.put("Exam Type ID", examTypeId);
		filters.put("Exam Schedule ID", examScheduleId);
		filters.put("Subject ID", subjectId);
		filters.put("Student ID", studentId);
		return filters;
	}

	private String normalizeFormat(String format) {
		return format == null ? "EXCEL" : format.trim().toUpperCase(Locale.ROOT);
	}
}
