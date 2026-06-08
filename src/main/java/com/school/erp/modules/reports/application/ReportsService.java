package com.school.erp.modules.reports.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogExportService;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.modules.api.dto.ModuleRecordSearchRequest;
import com.school.erp.common.modules.application.ModuleRecordService;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.fees.application.FeeImportExportService;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse.ReportFilterOption;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse.ReportTypeOption;
import com.school.erp.modules.students.application.StudentImportExportService;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportsService {

	private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

	private final StudentImportExportService studentImportExportService;
	private final AttendanceService attendanceService;
	private final FeeImportExportService feeImportExportService;
	private final AuditLogExportService auditLogExportService;
	private final AuditLogService auditLogService;
	private final ModuleRecordService moduleRecordService;

	public ReportOptionsResponse options() {
		return new ReportOptionsResponse(List.of(
				report(
						"STUDENT_REPORT",
						"Student Report",
						List.of("EXCEL", "CSV"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"ATTENDANCE_REPORT",
						"Attendance Report",
						List.of("CSV"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("FROM_DATE", "From Date", "DATE", true),
						filter("TO_DATE", "To Date", "DATE", true)),
				report(
						"FEE_COLLECTION_REPORT",
						"Fee Collection Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false),
						filter("PAYMENT_MODE", "Payment Mode", "SELECT", false)),
				report(
						"DEFAULTER_REPORT",
						"Defaulter Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false)),
				report(
						"EXAM_RESULT_REPORT",
						"Exam Result Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false),
						filter("EXAM_TYPE", "Exam Type", "SELECT", false),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", false)),
				report(
						"HOSTEL_OCCUPANCY_REPORT",
						"Hostel Occupancy Report",
						List.of("EXCEL", "CSV", "PDF")),
				report(
						"AUDIT_LOG_REPORT",
						"Audit Log Report",
						List.of("EXCEL", "CSV"),
						filter("MODULE", "Module", "TEXT", false),
						filter("ACTION", "Action", "TEXT", false),
						filter("PERFORMED_BY", "Performed By", "TEXT", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false))));
	}

	public ReportExportFile export(ReportExportRequest request) {
		String reportType = normalize(request.reportType());
		String format = normalize(request.format());
		ReportExportFile file = switch (reportType) {
			case "STUDENT_REPORT" -> studentExport(format);
			case "ATTENDANCE_REPORT" -> attendanceExport(format, request);
			case "FEE_COLLECTION_REPORT" -> file(
					feeImportExportService.collectionReport(format),
					"fee-collection-summary." + extension(format),
					contentType(format));
			case "DEFAULTER_REPORT" -> file(
					feeImportExportService.defaulterReport(format),
					"fee-defaulters." + extension(format),
					contentType(format));
			case "EXAM_RESULT_REPORT" -> moduleRecordExport("EXAMS", "results", format, "exam-results");
			case "HOSTEL_OCCUPANCY_REPORT" -> moduleRecordExport("HOSTEL", "allocations", format, "hostel-occupancy");
			case "AUDIT_LOG_REPORT" -> auditExport(format, request);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported report type: " + request.reportType());
		};
		auditLogService.recordStandalone(new AuditLogEvent(
				"REPORTS",
				"ReportExport",
				reportType,
				AuditAction.EXPORT,
				null,
				auditPayload(reportType, format, request, file)));
		return file;
	}

	private ReportTypeOption report(String code, String name, List<String> formats, ReportFilterOption... filters) {
		return new ReportTypeOption(code, name, formats, List.of(filters));
	}

	private ReportFilterOption filter(String code, String label, String type, boolean required) {
		return new ReportFilterOption(code, label, type, required);
	}

	private ReportExportFile studentExport(String format) {
		return switch (format) {
			case "EXCEL" -> file(
					studentImportExportService.exportExcel(),
					"students.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			case "CSV" -> file(studentImportExportService.exportCsv(), "students.csv", "text/csv");
			default -> unsupported("Student Report", format);
		};
	}

	private ReportExportFile attendanceExport(String format, ReportExportRequest request) {
		if (!"CSV".equals(format)) {
			return unsupported("Attendance Report", format);
		}
		return file(
				attendanceService.export(
						requiredUuid(request.academicYearId(), "Academic year"),
						requiredUuid(request.classId(), "Class"),
						requiredUuid(request.sectionId(), "Section"),
						requiredDate(request.fromDate(), "From date"),
						requiredDate(request.toDate(), "To date")),
				"attendance.csv",
				"text/csv");
	}

	private ReportExportFile auditExport(String format, ReportExportRequest request) {
		AuditLogSearchRequest search = new AuditLogSearchRequest(
				null,
				blankToNull(request.module()),
				null,
				null,
				blankToNull(request.action()),
				blankToNull(request.performedBy()),
				request.fromDate() == null ? null : request.fromDate().atStartOfDay(SCHOOL_ZONE).toInstant(),
				request.toDate() == null ? null : request.toDate().atTime(LocalTime.MAX).atZone(SCHOOL_ZONE).toInstant(),
				request.fromDate(),
				request.toDate());
		return switch (format) {
			case "EXCEL" -> file(
					auditLogExportService.exportExcel(search),
					"audit-logs.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			case "CSV" -> file(auditLogExportService.exportCsv(search), "audit-logs.csv", "text/csv");
			default -> unsupported("Audit Log Report", format);
		};
	}

	private ReportExportFile moduleRecordExport(String module, String recordType, String format, String filename) {
		ModuleRecordSearchRequest search = new ModuleRecordSearchRequest(null, null, null, null, null, null, null);
		byte[] content = switch (format) {
			case "EXCEL" -> moduleRecordService.exportExcel(module, recordType, search);
			case "CSV" -> moduleRecordService.exportCsv(module, recordType, search);
			case "PDF" -> moduleRecordService.exportPdf(module, recordType, search);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported export format: " + format);
		};
		return file(content, filename + "." + extension(format), contentType(format));
	}

	private UUID requiredUuid(UUID value, String label) {
		if (value == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, label + " is required.");
		}
		return value;
	}

	private LocalDate requiredDate(LocalDate value, String label) {
		if (value == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, label + " is required.");
		}
		return value;
	}

	private ReportExportFile unsupported(String reportName, String format) {
		throw new BusinessException(ErrorCode.VALIDATION_ERROR, reportName + " does not support " + format + " export.");
	}

	private ReportExportFile file(byte[] content, String filename, String contentType) {
		return new ReportExportFile(content, filename, contentType);
	}

	private String normalize(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Report type and format are required.");
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private String extension(String format) {
		return switch (format) {
			case "PDF" -> "pdf";
			case "CSV" -> "csv";
			default -> "xlsx";
		};
	}

	private String contentType(String format) {
		return switch (format) {
			case "PDF" -> "application/pdf";
			case "CSV" -> "text/csv";
			default -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
		};
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Map<String, Object> auditPayload(
			String reportType,
			String format,
			ReportExportRequest request,
			ReportExportFile file) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reportType", reportType);
		payload.put("format", format);
		payload.put("filename", file.filename());
		putIfPresent(payload, "academicYearId", request.academicYearId());
		putIfPresent(payload, "classId", request.classId());
		putIfPresent(payload, "sectionId", request.sectionId());
		putIfPresent(payload, "fromDate", request.fromDate());
		putIfPresent(payload, "toDate", request.toDate());
		putIfPresent(payload, "paymentMode", blankToNull(request.paymentMode()));
		putIfPresent(payload, "examTypeId", request.examTypeId());
		putIfPresent(payload, "examScheduleId", request.examScheduleId());
		putIfPresent(payload, "module", blankToNull(request.module()));
		putIfPresent(payload, "action", blankToNull(request.action()));
		putIfPresent(payload, "performedBy", blankToNull(request.performedBy()));
		return payload;
	}

	private void putIfPresent(Map<String, Object> payload, String key, Object value) {
		if (value != null) {
			payload.put(key, value);
		}
	}

	public record ReportExportRequest(
			String reportType,
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate fromDate,
			LocalDate toDate,
			String paymentMode,
			UUID examTypeId,
			UUID examScheduleId,
			String module,
			String action,
			String performedBy) {
	}

	public record ReportExportFile(byte[] content, String filename, String contentType) {
	}
}
