package com.school.erp.modules.reports.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.api.dto.AuditLogDto;
import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogExportService;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.api.dto.SectionResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.exams.application.ExamReportExportService;
import com.school.erp.modules.fees.api.dto.DefaulterSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeReportRequest;
import com.school.erp.modules.fees.application.FeeImportExportService;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.hostel.application.HostelReportExportService;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.library.application.LibraryReportExportService;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse.ReportFilterOption;
import com.school.erp.modules.reports.api.dto.ReportOptionsResponse.ReportTypeOption;
import com.school.erp.modules.staff.application.StaffReportExportService;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.application.StudentImportExportService;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.transport.application.TransportReportExportService;
import com.school.erp.modules.transport.domain.TransportStatus;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportsService {

	private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");
	private static final List<String> ACADEMIC_STRUCTURE_HEADERS = List.of(
			"Level",
			"Academic Year",
			"Academic Year Code",
			"Class",
			"Class Code",
			"Section",
			"Section Code",
			"Capacity",
			"Status");

	private final StudentImportExportService studentImportExportService;
	private final AcademicHierarchyService academicHierarchyService;
	private final AttendanceService attendanceService;
	private final FeeImportExportService feeImportExportService;
	private final ExamReportExportService examReportExportService;
	private final HostelReportExportService hostelReportExportService;
	private final TransportReportExportService transportReportExportService;
	private final StaffReportExportService staffReportExportService;
	private final LibraryReportExportService libraryReportExportService;
	private final AuditLogExportService auditLogExportService;
	private final AuditLogService auditLogService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	public ReportOptionsResponse options() {
		return new ReportOptionsResponse(List.of(
				report(
						"STUDENT_REPORT",
						"Student Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"ACADEMIC_STRUCTURE_REPORT",
						"Academic Structure Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false)),
				report(
						"ATTENDANCE_REPORT",
						"Attendance Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("FROM_DATE", "From Date", "DATE", true),
						filter("TO_DATE", "To Date", "DATE", true)),
				report(
						"STAFF_LIST_REPORT",
						"Staff List Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("STAFF_TYPE", "Staff Type", "SELECT", false),
						filter("DEPARTMENT", "Department", "SELECT", false),
						filter("DESIGNATION", "Designation", "SELECT", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"STAFF_ATTENDANCE_REPORT",
						"Staff Attendance Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("DEPARTMENT", "Department", "SELECT", false),
						filter("DESIGNATION", "Designation", "SELECT", false),
						filter("FROM_DATE", "From Date", "DATE", true),
						filter("TO_DATE", "To Date", "DATE", true)),
				report(
						"STAFF_LEAVE_REPORT",
						"Staff Leave Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("STAFF", "Staff", "SELECT", false),
						filter("STATUS", "Status", "SELECT", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false)),
				report(
						"STAFF_PAYROLL_REPORT",
						"Staff Payroll Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("STAFF", "Staff", "SELECT", false),
						filter("PAYROLL_YEAR", "Payroll Year", "NUMBER", false),
						filter("PAYROLL_MONTH", "Payroll Month", "NUMBER", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"FEE_COLLECTION_REPORT",
						"Fee Collection Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"DEFAULTER_REPORT",
						"Defaulter Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("CLASS", "Class", "CLASS", false),
						filter("SECTION", "Section / Division", "SECTION", false)),
				report(
						"EXAM_SCHEDULE_REPORT",
						"Exam Schedule Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("EXAM_TYPE", "Exam Type", "SELECT", false),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", false),
						filter("SUBJECT", "Subject", "SELECT", false)),
				report(
						"EXAM_MARKS_REPORT",
						"Exam Marks Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", true),
						filter("SUBJECT", "Subject", "SELECT", true),
						filter("STUDENT", "Student", "SELECT", false)),
				report(
						"EXAM_RESULT_REPORT",
						"Exam Result Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("EXAM_TYPE", "Exam Type", "SELECT", false),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", false),
						filter("SUBJECT", "Subject", "SELECT", false),
						filter("STUDENT", "Student", "SELECT", false)),
				report(
						"EXAM_PASS_FAIL_SUMMARY_REPORT",
						"Exam Pass/Fail Summary",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("EXAM_TYPE", "Exam Type", "SELECT", false),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", false),
						filter("SUBJECT", "Subject", "SELECT", false)),
				report(
						"EXAM_GRADE_SUMMARY_REPORT",
						"Exam Grade Summary",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("CLASS", "Class", "CLASS", true),
						filter("SECTION", "Section / Division", "SECTION", true),
						filter("EXAM_TYPE", "Exam Type", "SELECT", false),
						filter("EXAM_SCHEDULE", "Exam Schedule", "SELECT", false),
						filter("SUBJECT", "Subject", "SELECT", false)),
				report(
						"HOSTEL_LIST_REPORT",
						"Hostel List",
						List.of("EXCEL", "CSV", "PDF")),
				report(
						"HOSTEL_OCCUPANCY_REPORT",
						"Hostel Occupancy Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("HOSTEL", "Hostel", "SELECT", false),
						filter("ROOM", "Room", "SELECT", false)),
				report(
						"HOSTEL_ALLOCATION_REPORT",
						"Hostel Allocation Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("HOSTEL", "Hostel", "SELECT", false),
						filter("ROOM", "Room", "SELECT", false),
						filter("STUDENT", "Student", "SELECT", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"TRANSPORT_ROUTES_REPORT",
						"Transport Routes",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("VEHICLE", "Vehicle", "SELECT", false)),
				report(
						"TRANSPORT_VEHICLES_REPORT",
						"Transport Vehicles",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", true),
						filter("VEHICLE", "Vehicle", "SELECT", false)),
				report(
						"TRANSPORT_DRIVERS_REPORT",
						"Transport Drivers",
						List.of("EXCEL", "CSV", "PDF")),
				report(
						"TRANSPORT_ASSIGNMENT_REPORT",
						"Transport Assignment Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("ACADEMIC_YEAR", "Academic Year", "ACADEMIC_YEAR", false),
						filter("VEHICLE", "Vehicle", "SELECT", false),
						filter("ROUTE", "Route", "SELECT", false),
						filter("STUDENT", "Student", "SELECT", false),
						filter("STATUS", "Status", "SELECT", false)),
				report(
						"LIBRARY_INVENTORY_REPORT",
						"Library Inventory Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_CATEGORY", "Category", "SELECT", false),
						filter("LIBRARY_PUBLISHER", "Publisher", "SELECT", false),
						filter("STATUS", "Active Status", "SELECT", false)),
				report(
						"LIBRARY_AVAILABLE_BOOKS_REPORT",
						"Library Available Books",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_BOOK", "Book", "SELECT", false),
						filter("KEYWORD", "Keyword", "TEXT", false)),
				report(
						"LIBRARY_ISSUED_BOOKS_REPORT",
						"Library Issued Books",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_BOOK", "Book", "SELECT", false),
						filter("LIBRARY_MEMBERSHIP", "Membership", "SELECT", false),
						filter("MEMBER_TYPE", "Member Type", "SELECT", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false)),
				report(
						"LIBRARY_OVERDUE_BOOKS_REPORT",
						"Library Overdue Books",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_BOOK", "Book", "SELECT", false),
						filter("LIBRARY_MEMBERSHIP", "Membership", "SELECT", false),
						filter("MEMBER_TYPE", "Member Type", "SELECT", false)),
				report(
						"LIBRARY_MEMBER_LOAN_HISTORY_REPORT",
						"Library Member Loan History",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_MEMBERSHIP", "Membership", "SELECT", true),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false)),
				report(
						"LIBRARY_FINE_REPORT",
						"Library Fine Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_MEMBERSHIP", "Membership", "SELECT", false),
						filter("STATUS", "Fine Status", "SELECT", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false)),
				report(
						"LIBRARY_LOST_DAMAGED_REPORT",
						"Library Lost/Damaged Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("LIBRARY_BOOK", "Book", "SELECT", false)),
				report(
						"AUDIT_LOG_REPORT",
						"Audit Log Report",
						List.of("EXCEL", "CSV", "PDF"),
						filter("MODULE", "Module", "TEXT", false),
						filter("ACTION", "Action", "TEXT", false),
						filter("PERFORMED_BY", "Performed By", "TEXT", false),
						filter("FROM_DATE", "From Date", "DATE", false),
						filter("TO_DATE", "To Date", "DATE", false))));
	}

	public ReportExportFile export(ReportExportRequest request) {
		String reportType = normalizeReportType(request.reportType());
		String format = normalizeFormat(request.format());
		authorize(reportType);
		ReportExportFile file = switch (reportType) {
			case "STUDENT_REPORT" -> studentExport(format, request);
			case "ACADEMIC_STRUCTURE_REPORT" -> academicStructureExport(format, request);
			case "ATTENDANCE_REPORT" -> attendanceExport(format, request);
			case "STAFF_LIST_REPORT" -> file(
					staffReportExportService.staffListReport(
							format,
							employmentStatus(request.status()),
							staffType(request.staffType()),
							request.departmentId(),
							request.designationId()),
					"staff-list." + extension(format),
					contentType(format));
			case "STAFF_ATTENDANCE_REPORT" -> file(
					staffReportExportService.staffAttendanceReport(
							format,
							requiredDate(request.fromDate(), "From date"),
							requiredDate(request.toDate(), "To date"),
							request.departmentId(),
							request.designationId()),
					"staff-attendance." + extension(format),
					contentType(format));
			case "STAFF_LEAVE_REPORT" -> file(
					staffReportExportService.staffLeaveReport(
							format,
							request.staffId(),
							leaveStatus(request.status()),
							request.fromDate(),
							request.toDate()),
					"staff-leaves." + extension(format),
					contentType(format));
			case "STAFF_PAYROLL_REPORT" -> file(
					staffReportExportService.staffPayrollReport(
							format,
							request.staffId(),
							request.payrollYear(),
							request.payrollMonth(),
							payrollStatus(request.status())),
					"staff-payroll." + extension(format),
					contentType(format));
			case "FEE_COLLECTION_REPORT" -> file(
					feeImportExportService.collectionReport(format, feeReportRequest(request)),
					"fee-collection-summary." + extension(format),
					contentType(format));
			case "DEFAULTER_REPORT" -> file(
					feeImportExportService.defaulterReport(format, defaulterSearchRequest(request)),
					"fee-defaulters." + extension(format),
					contentType(format));
			case "EXAM_SCHEDULE_REPORT" -> file(
					examReportExportService.scheduleReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							requiredUuid(request.classId(), "Class"),
							requiredUuid(request.sectionId(), "Section"),
							request.examTypeId(),
							request.examScheduleId(),
							request.subjectId()),
					"exam-schedule-report." + extension(format),
					contentType(format));
			case "EXAM_MARKS_REPORT" -> file(
					examReportExportService.marksReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							requiredUuid(request.classId(), "Class"),
							requiredUuid(request.sectionId(), "Section"),
							requiredUuid(request.examScheduleId(), "Exam schedule"),
							requiredUuid(request.subjectId(), "Subject"),
							request.studentId()),
					"exam-marks-report." + extension(format),
					contentType(format));
			case "EXAM_RESULT_REPORT" -> file(
					examReportExportService.resultReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							requiredUuid(request.classId(), "Class"),
							requiredUuid(request.sectionId(), "Section"),
							request.examTypeId(),
							request.examScheduleId(),
							request.subjectId(),
							request.studentId()),
					"exam-results." + extension(format),
					contentType(format));
			case "EXAM_PASS_FAIL_SUMMARY_REPORT" -> file(
					examReportExportService.passFailSummaryReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							requiredUuid(request.classId(), "Class"),
							requiredUuid(request.sectionId(), "Section"),
							request.examTypeId(),
							request.examScheduleId(),
							request.subjectId(),
							request.studentId()),
					"exam-pass-fail-summary." + extension(format),
					contentType(format));
			case "EXAM_GRADE_SUMMARY_REPORT" -> file(
					examReportExportService.gradeSummaryReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							requiredUuid(request.classId(), "Class"),
							requiredUuid(request.sectionId(), "Section"),
							request.examTypeId(),
							request.examScheduleId(),
							request.subjectId(),
							request.studentId()),
					"exam-grade-summary." + extension(format),
					contentType(format));
			case "HOSTEL_LIST_REPORT" -> file(
					hostelReportExportService.hostelListReport(format),
					"hostel-list." + extension(format),
					contentType(format));
			case "HOSTEL_OCCUPANCY_REPORT" -> file(
					hostelReportExportService.occupancyReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							request.hostelId(),
							request.roomId()),
					"hostel-occupancy." + extension(format),
					contentType(format));
			case "HOSTEL_ALLOCATION_REPORT" -> file(
					hostelReportExportService.allocationReport(
							format,
							request.academicYearId(),
							request.hostelId(),
							request.roomId(),
							request.studentId(),
							hostelStatus(request.status())),
					"hostel-allocations." + extension(format),
					contentType(format));
			case "TRANSPORT_ROUTES_REPORT" -> file(
					transportReportExportService.routesReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							request.vehicleId()),
					"transport-routes." + extension(format),
					contentType(format));
			case "TRANSPORT_VEHICLES_REPORT" -> file(
					transportReportExportService.vehiclesReport(
							format,
							requiredUuid(request.academicYearId(), "Academic year"),
							request.vehicleId()),
					"transport-vehicles." + extension(format),
					contentType(format));
			case "TRANSPORT_DRIVERS_REPORT" -> file(
					transportReportExportService.driversReport(format),
					"transport-drivers." + extension(format),
					contentType(format));
			case "TRANSPORT_ASSIGNMENT_REPORT" -> file(
					transportReportExportService.assignmentsReport(
							format,
							request.academicYearId(),
							request.vehicleId(),
							request.routeId(),
							request.studentId(),
							transportStatus(request.status())),
					"transport-assignments." + extension(format),
					contentType(format));
			case "LIBRARY_INVENTORY_REPORT" -> file(
					libraryReportExportService.inventoryReport(
							format,
							request.libraryCategoryId(),
							request.libraryPublisherId(),
							activeStatus(request.status())),
					"library-inventory." + extension(format),
					contentType(format));
			case "LIBRARY_AVAILABLE_BOOKS_REPORT" -> file(
					libraryReportExportService.availableBooksReport(format, request.libraryBookId(), request.libraryKeyword()),
					"library-available-books." + extension(format),
					contentType(format));
			case "LIBRARY_ISSUED_BOOKS_REPORT" -> file(
					libraryReportExportService.issuedBooksReport(
							format,
							request.libraryMembershipId(),
							request.libraryBookId(),
							libraryMemberType(request.memberType()),
							request.fromDate(),
							request.toDate()),
					"library-issued-books." + extension(format),
					contentType(format));
			case "LIBRARY_OVERDUE_BOOKS_REPORT" -> file(
					libraryReportExportService.overdueBooksReport(
							format,
							request.libraryMembershipId(),
							request.libraryBookId(),
							libraryMemberType(request.memberType())),
					"library-overdue-books." + extension(format),
					contentType(format));
			case "LIBRARY_MEMBER_LOAN_HISTORY_REPORT" -> file(
					libraryReportExportService.memberLoanHistoryReport(
							format,
							requiredUuid(request.libraryMembershipId(), "Library membership"),
							request.fromDate(),
							request.toDate()),
					"library-member-loan-history." + extension(format),
					contentType(format));
			case "LIBRARY_FINE_REPORT" -> file(
					libraryReportExportService.fineReport(
							format,
							libraryFineStatus(request.status()),
							request.libraryMembershipId(),
							request.fromDate(),
							request.toDate()),
					"library-fines." + extension(format),
					contentType(format));
			case "LIBRARY_LOST_DAMAGED_REPORT" -> file(
					libraryReportExportService.lostDamagedReport(format, request.libraryBookId()),
					"library-lost-damaged." + extension(format),
					contentType(format));
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

	public PageResponse<Map<String, Object>> preview(ReportExportRequest request, PageRequestDto pageRequest) {
		String reportType = normalizeReportType(request.reportType());
		authorize(reportType);
		PageRequestDto effectivePageRequest = pageRequest == null ? new PageRequestDto(0, 20, null, null) : pageRequest;
		if ("AUDIT_LOG_REPORT".equals(reportType)) {
			return auditPreview(request, effectivePageRequest);
		}
		return page(rows(reportType, request), effectivePageRequest);
	}

	private ReportTypeOption report(String code, String name, List<String> formats, ReportFilterOption... filters) {
		return new ReportTypeOption(code, name, formats, List.of(filters));
	}

	private ReportFilterOption filter(String code, String label, String type, boolean required) {
		return new ReportFilterOption(code, label, type, required);
	}

	private ReportExportFile studentExport(String format, ReportExportRequest request) {
		StudentSearchRequest search = studentSearchRequest(request);
		return switch (format) {
			case "EXCEL" -> file(
					studentImportExportService.exportExcel(search),
					"students.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			case "CSV" -> file(studentImportExportService.exportCsv(search), "students.csv", "text/csv");
			case "PDF" -> file(studentImportExportService.exportPdf(search), "students.pdf", "application/pdf");
			default -> unsupported("Student Report", format);
		};
	}

	private ReportExportFile attendanceExport(String format, ReportExportRequest request) {
		List<Map<String, Object>> rows = attendanceRows(request);
		Map<String, Object> filters = attendanceFilters(request);
		byte[] content = switch (format) {
			case "EXCEL" -> excelExportService.export("attendance-report", AttendanceService.ATTENDANCE_REPORT_HEADERS, rows);
			case "CSV" -> csvExportService.export(AttendanceService.ATTENDANCE_REPORT_HEADERS, rows);
			case "PDF" -> pdfExportService.exportTable("Attendance Report", filters, AttendanceService.ATTENDANCE_REPORT_HEADERS, rows);
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Attendance Report does not support " + format + " export.");
		};
		return file(content, "attendance." + extension(format), contentType(format));
	}

	private ReportExportFile academicStructureExport(String format, ReportExportRequest request) {
		List<Map<String, Object>> rows = academicStructureRows(request);
		Map<String, Object> filters = academicStructureFilters(request);
		byte[] content = switch (format) {
			case "EXCEL" -> excelExportService.export("academic-structure-report", ACADEMIC_STRUCTURE_HEADERS, rows);
			case "CSV" -> csvExportService.export(ACADEMIC_STRUCTURE_HEADERS, rows);
			case "PDF" -> pdfExportService.exportTable("Academic Structure Report", filters, ACADEMIC_STRUCTURE_HEADERS, rows);
			default -> throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Academic Structure Report does not support " + format + " export.");
		};
		return file(content, "academic-structure." + extension(format), contentType(format));
	}

	private ReportExportFile auditExport(String format, ReportExportRequest request) {
		AuditLogSearchRequest search = auditSearchRequest(request);
		return switch (format) {
			case "EXCEL" -> file(
					auditLogExportService.exportExcel(search),
					"audit-logs.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			case "CSV" -> file(auditLogExportService.exportCsv(search), "audit-logs.csv", "text/csv");
			case "PDF" -> file(auditLogExportService.exportPdf(search), "audit-logs.pdf", "application/pdf");
			default -> unsupported("Audit Log Report", format);
		};
	}

	private List<Map<String, Object>> rows(String reportType, ReportExportRequest request) {
		return switch (reportType) {
			case "STUDENT_REPORT" -> studentImportExportService.reportRows(studentSearchRequest(request));
			case "ACADEMIC_STRUCTURE_REPORT" -> academicStructureRows(request);
			case "ATTENDANCE_REPORT" -> attendanceRows(request);
			case "STAFF_LIST_REPORT" -> staffReportExportService.staffListRows(
					employmentStatus(request.status()),
					staffType(request.staffType()),
					request.departmentId(),
					request.designationId());
			case "STAFF_ATTENDANCE_REPORT" -> staffReportExportService.staffAttendanceRows(
					requiredDate(request.fromDate(), "From date"),
					requiredDate(request.toDate(), "To date"),
					request.departmentId(),
					request.designationId());
			case "STAFF_LEAVE_REPORT" -> staffReportExportService.staffLeaveRows(
					request.staffId(),
					leaveStatus(request.status()),
					request.fromDate(),
					request.toDate());
			case "STAFF_PAYROLL_REPORT" -> staffReportExportService.staffPayrollRows(
					request.staffId(),
					request.payrollYear(),
					request.payrollMonth(),
					payrollStatus(request.status()));
			case "FEE_COLLECTION_REPORT" -> feeImportExportService.collectionReportRows(feeReportRequest(request));
			case "DEFAULTER_REPORT" -> feeImportExportService.defaulterReportRows(defaulterSearchRequest(request));
			case "EXAM_SCHEDULE_REPORT" -> examReportExportService.scheduleRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					requiredUuid(request.classId(), "Class"),
					requiredUuid(request.sectionId(), "Section"),
					request.examTypeId(),
					request.examScheduleId(),
					request.subjectId());
			case "EXAM_MARKS_REPORT" -> examReportExportService.marksRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					requiredUuid(request.classId(), "Class"),
					requiredUuid(request.sectionId(), "Section"),
					requiredUuid(request.examScheduleId(), "Exam schedule"),
					requiredUuid(request.subjectId(), "Subject"),
					request.studentId());
			case "EXAM_RESULT_REPORT" -> examReportExportService.resultRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					requiredUuid(request.classId(), "Class"),
					requiredUuid(request.sectionId(), "Section"),
					request.examTypeId(),
					request.examScheduleId(),
					request.subjectId(),
					request.studentId());
			case "EXAM_PASS_FAIL_SUMMARY_REPORT" -> examReportExportService.passFailRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					requiredUuid(request.classId(), "Class"),
					requiredUuid(request.sectionId(), "Section"),
					request.examTypeId(),
					request.examScheduleId(),
					request.subjectId(),
					request.studentId());
			case "EXAM_GRADE_SUMMARY_REPORT" -> examReportExportService.gradeRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					requiredUuid(request.classId(), "Class"),
					requiredUuid(request.sectionId(), "Section"),
					request.examTypeId(),
					request.examScheduleId(),
					request.subjectId(),
					request.studentId());
			case "HOSTEL_LIST_REPORT" -> hostelReportExportService.hostelListRows();
			case "HOSTEL_OCCUPANCY_REPORT" -> hostelReportExportService.occupancyRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					request.hostelId(),
					request.roomId());
			case "HOSTEL_ALLOCATION_REPORT" -> hostelReportExportService.allocationRows(
					request.academicYearId(),
					request.hostelId(),
					request.roomId(),
					request.studentId(),
					hostelStatus(request.status()));
			case "TRANSPORT_ROUTES_REPORT" -> transportReportExportService.routeRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					request.vehicleId());
			case "TRANSPORT_VEHICLES_REPORT" -> transportReportExportService.vehicleRows(
					requiredUuid(request.academicYearId(), "Academic year"),
					request.vehicleId());
			case "TRANSPORT_DRIVERS_REPORT" -> transportReportExportService.driverRows();
			case "TRANSPORT_ASSIGNMENT_REPORT" -> transportReportExportService.assignmentRows(
					request.academicYearId(),
					request.vehicleId(),
					request.routeId(),
					request.studentId(),
					transportStatus(request.status()));
			case "LIBRARY_INVENTORY_REPORT" -> libraryReportExportService.inventoryRows(
					request.libraryCategoryId(),
					request.libraryPublisherId(),
					activeStatus(request.status()));
			case "LIBRARY_AVAILABLE_BOOKS_REPORT" -> libraryReportExportService.availableRows(
					request.libraryBookId(),
					request.libraryKeyword());
			case "LIBRARY_ISSUED_BOOKS_REPORT" -> libraryReportExportService.issuedRows(
					request.libraryMembershipId(),
					request.libraryBookId(),
					libraryMemberType(request.memberType()),
					request.fromDate(),
					request.toDate());
			case "LIBRARY_OVERDUE_BOOKS_REPORT" -> libraryReportExportService.overdueRows(
					request.libraryMembershipId(),
					request.libraryBookId(),
					libraryMemberType(request.memberType()));
			case "LIBRARY_MEMBER_LOAN_HISTORY_REPORT" -> libraryReportExportService.memberLoanHistoryRows(
					requiredUuid(request.libraryMembershipId(), "Library membership"),
					request.fromDate(),
					request.toDate());
			case "LIBRARY_FINE_REPORT" -> libraryReportExportService.fineRows(
					libraryFineStatus(request.status()),
					request.libraryMembershipId(),
					request.fromDate(),
					request.toDate());
			case "LIBRARY_LOST_DAMAGED_REPORT" -> libraryReportExportService.lostDamagedRows(request.libraryBookId());
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported report type: " + request.reportType());
		};
	}

	private List<Map<String, Object>> attendanceRows(ReportExportRequest request) {
		return attendanceService.reportRows(
				requiredUuid(request.academicYearId(), "Academic year"),
				requiredUuid(request.classId(), "Class"),
				requiredUuid(request.sectionId(), "Section"),
				requiredDate(request.fromDate(), "From date"),
				requiredDate(request.toDate(), "To date"));
	}

	private Map<String, Object> attendanceFilters(ReportExportRequest request) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Academic Year ID", request.academicYearId());
		filters.put("Class ID", request.classId());
		filters.put("Section ID", request.sectionId());
		filters.put("From Date", request.fromDate());
		filters.put("To Date", request.toDate());
		return filters;
	}

	private List<Map<String, Object>> academicStructureRows(ReportExportRequest request) {
		if (request.sectionId() != null && request.classId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class is required when section is provided.");
		}
		if (request.classId() != null) {
			ClassResponse classResponse = academicHierarchyService.getClass(request.classId());
			AcademicYearResponse academicYear = academicHierarchyService.getAcademicYear(classResponse.academicYearId());
			if (request.academicYearId() != null && !request.academicYearId().equals(classResponse.academicYearId())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Class does not belong to the selected academic year.");
			}
			List<Map<String, Object>> rows = new ArrayList<>();
			addClassRows(rows, academicYear, classResponse, request.sectionId());
			return rows;
		}
		List<AcademicYearResponse> academicYears = request.academicYearId() == null
				? academicHierarchyService.getAcademicYears()
				: List.of(academicHierarchyService.getAcademicYear(request.academicYearId()));
		List<Map<String, Object>> rows = new ArrayList<>();
		for (AcademicYearResponse academicYear : academicYears) {
			rows.add(academicYearRow(academicYear));
			for (ClassResponse classResponse : academicHierarchyService.getClasses(academicYear.id())) {
				addClassRows(rows, academicYear, classResponse, null);
			}
		}
		return rows;
	}

	private void addClassRows(
			List<Map<String, Object>> rows,
			AcademicYearResponse academicYear,
			ClassResponse classResponse,
			UUID sectionId) {
		rows.add(classRow(academicYear, classResponse));
		if (sectionId != null) {
			SectionEntity section = academicHierarchyService.loadSectionForClass(classResponse.id(), sectionId);
			rows.add(sectionRow(
					academicYear,
					classResponse,
					section.getName(),
					section.getCode(),
					section.getCapacity(),
					section.isActive()));
			return;
		}
		for (SectionResponse section : academicHierarchyService.getSections(classResponse.id())) {
			rows.add(sectionRow(
					academicYear,
					classResponse,
					section.name(),
					section.code(),
					section.capacity(),
					section.active()));
		}
	}

	private Map<String, Object> academicYearRow(AcademicYearResponse academicYear) {
		return academicStructureRow(
				"Academic Year",
				academicYear.name(),
				academicYear.code(),
				null,
				null,
				null,
				null,
				null,
				academicYear.active() ? "ACTIVE" : "INACTIVE");
	}

	private Map<String, Object> classRow(AcademicYearResponse academicYear, ClassResponse classResponse) {
		return academicStructureRow(
				"Class",
				academicYear.name(),
				academicYear.code(),
				classResponse.name(),
				classResponse.code(),
				null,
				null,
				null,
				classResponse.active() ? "ACTIVE" : "INACTIVE");
	}

	private Map<String, Object> sectionRow(
			AcademicYearResponse academicYear,
			ClassResponse classResponse,
			String sectionName,
			String sectionCode,
			Integer capacity,
			boolean active) {
		return academicStructureRow(
				"Section",
				academicYear.name(),
				academicYear.code(),
				classResponse.name(),
				classResponse.code(),
				sectionName,
				sectionCode,
				capacity,
				active ? "ACTIVE" : "INACTIVE");
	}

	private Map<String, Object> academicStructureRow(
			String level,
			String academicYear,
			String academicYearCode,
			String className,
			String classCode,
			String sectionName,
			String sectionCode,
			Integer capacity,
			String status) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Level", level);
		row.put("Academic Year", academicYear);
		row.put("Academic Year Code", academicYearCode);
		row.put("Class", className);
		row.put("Class Code", classCode);
		row.put("Section", sectionName);
		row.put("Section Code", sectionCode);
		row.put("Capacity", capacity);
		row.put("Status", status);
		return row;
	}

	private Map<String, Object> academicStructureFilters(ReportExportRequest request) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Academic Year ID", request.academicYearId());
		filters.put("Class ID", request.classId());
		filters.put("Section ID", request.sectionId());
		return filters;
	}

	private StudentSearchRequest studentSearchRequest(ReportExportRequest request) {
		return new StudentSearchRequest(
				null,
				studentStatus(request.status()),
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				null,
				null,
				null,
				null,
				null);
	}

	private FeeReportRequest feeReportRequest(ReportExportRequest request) {
		if (request.sectionId() != null && request.classId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class is required when section is provided.");
		}
		AcademicYear academicYear = request.academicYearId() == null
				? null
				: academicHierarchyService.loadAcademicYear(request.academicYearId());
		ClassEntity classEntity = request.classId() == null
				? null
				: academicHierarchyService.loadClass(request.classId());
		SectionEntity section = request.sectionId() == null
				? null
				: academicHierarchyService.loadSectionForClass(request.classId(), request.sectionId());
		return new FeeReportRequest(
				academicYear == null ? null : academicYear.getName(),
				classEntity == null ? null : classEntity.getName(),
				section == null ? null : section.getName(),
				feeStatus(request.status()));
	}

	private DefaulterSearchRequest defaulterSearchRequest(ReportExportRequest request) {
		return new DefaulterSearchRequest(
				null,
				null,
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private AuditLogSearchRequest auditSearchRequest(ReportExportRequest request) {
		return new AuditLogSearchRequest(
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
	}

	private PageResponse<Map<String, Object>> auditPreview(
			ReportExportRequest request,
			PageRequestDto pageRequest) {
		PageResponse<AuditLogDto> source = auditLogService.search(auditSearchRequest(request), pageRequest);
		return new PageResponse<>(
				source.content().stream().map(this::auditRow).toList(),
				source.page(),
				source.size(),
				source.totalElements(),
				source.totalPages(),
				source.first(),
				source.last());
	}

	private Map<String, Object> auditRow(AuditLogDto auditLog) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Module", auditLog.moduleName());
		row.put("Entity", auditLog.entityName());
		row.put("Entity ID", auditLog.entityId());
		row.put("Action", auditLog.action());
		row.put("Performed By", auditLog.performedBy());
		row.put("Performed At", auditLog.performedAt());
		row.put("IP Address", auditLog.ipAddress());
		return row;
	}

	private PageResponse<Map<String, Object>> page(List<Map<String, Object>> rows, PageRequestDto pageRequest) {
		int page = pageRequest.page();
		int size = pageRequest.size();
		int from = Math.min(page * size, rows.size());
		int to = Math.min(from + size, rows.size());
		int totalPages = rows.isEmpty() ? 0 : (int) Math.ceil((double) rows.size() / size);
		return new PageResponse<>(
				rows.subList(from, to),
				page,
				size,
				rows.size(),
				totalPages,
				page == 0,
				totalPages == 0 || page >= totalPages - 1);
	}

	private FeeAssignmentStatus feeStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return FeeAssignmentStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported fee status: " + value);
		}
	}

	private StudentStatus studentStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return StudentStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported student status: " + value);
		}
	}

	private EmploymentStatus employmentStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return EmploymentStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported staff status: " + value);
		}
	}

	private StaffType staffType(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return StaffType.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported staff type: " + value);
		}
	}

	private LeaveStatus leaveStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return LeaveStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported leave status: " + value);
		}
	}

	private PayrollStatus payrollStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return PayrollStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported payroll status: " + value);
		}
	}

	private HostelAllocationStatus hostelStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return HostelAllocationStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported hostel allocation status: " + value);
		}
	}

	private TransportStatus transportStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return TransportStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported transport status: " + value);
		}
	}

	private LibraryMemberType libraryMemberType(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return LibraryMemberType.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported library member type: " + value);
		}
	}

	private LibraryFineStatus libraryFineStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return LibraryFineStatus.valueOf(normalized.toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported library fine status: " + value);
		}
	}

	private Boolean activeStatus(String value) {
		String normalized = blankToNull(value);
		if (normalized == null) {
			return null;
		}
		return switch (normalized.toUpperCase(Locale.ROOT)) {
			case "ACTIVE", "TRUE", "YES" -> true;
			case "INACTIVE", "FALSE", "NO" -> false;
			default -> throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Unsupported active status: " + value);
		};
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

	private String normalizeReportType(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Report type is required.");
		}
		return value.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeFormat(String value) {
		if (value == null || value.isBlank()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Report format is required.");
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

	private void authorize(String reportType) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		if (!hasAuthority(authentication, "REPORTS_READ")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Reports access is required.");
		}
		String[] requiredAuthorities = switch (reportType) {
			case "STUDENT_REPORT" -> new String[] { "STUDENTS_READ", "ACADEMIC_READ" };
			case "ACADEMIC_STRUCTURE_REPORT" -> new String[] { "ACADEMIC_READ" };
			case "ATTENDANCE_REPORT" -> new String[] { "ATTENDANCE_READ" };
			case "STAFF_LIST_REPORT" -> new String[] { "STAFF_READ" };
			case "STAFF_ATTENDANCE_REPORT" -> new String[] { "ATTENDANCE_READ" };
			case "STAFF_LEAVE_REPORT" -> new String[] { "LEAVE_READ" };
			case "STAFF_PAYROLL_REPORT" -> new String[] { "PAYROLL_READ" };
			case "FEE_COLLECTION_REPORT", "DEFAULTER_REPORT" -> new String[] { "FEES_READ" };
			case "EXAM_SCHEDULE_REPORT", "EXAM_MARKS_REPORT", "EXAM_RESULT_REPORT",
					"EXAM_PASS_FAIL_SUMMARY_REPORT", "EXAM_GRADE_SUMMARY_REPORT" -> new String[] { "EXAMS_READ", "ACADEMIC_READ" };
			case "HOSTEL_LIST_REPORT", "HOSTEL_OCCUPANCY_REPORT", "HOSTEL_ALLOCATION_REPORT" -> new String[] { "HOSTEL_READ" };
			case "TRANSPORT_ROUTES_REPORT", "TRANSPORT_VEHICLES_REPORT", "TRANSPORT_DRIVERS_REPORT",
					"TRANSPORT_ASSIGNMENT_REPORT" -> new String[] { "TRANSPORT_READ" };
			case "LIBRARY_INVENTORY_REPORT", "LIBRARY_AVAILABLE_BOOKS_REPORT", "LIBRARY_ISSUED_BOOKS_REPORT",
					"LIBRARY_OVERDUE_BOOKS_REPORT", "LIBRARY_MEMBER_LOAN_HISTORY_REPORT", "LIBRARY_FINE_REPORT",
					"LIBRARY_LOST_DAMAGED_REPORT" -> new String[] { "LIBRARY_READ" };
			case "AUDIT_LOG_REPORT" -> new String[] { "AUDIT_LOGS_READ" };
			default -> new String[0];
		};
		if (requiredAuthorities.length > 0 && !hasAnyAuthority(authentication, requiredAuthorities)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Report requires one of: " + String.join(", ", requiredAuthorities));
		}
	}

	private boolean hasAnyAuthority(Authentication authentication, String... authorities) {
		for (String authority : authorities) {
			if (hasAuthority(authentication, authority)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAuthority(Authentication authentication, String authority) {
		return authentication.getAuthorities().stream()
				.anyMatch(granted -> granted.getAuthority().equals(authority));
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
		putIfPresent(payload, "status", blankToNull(request.status()));
		putIfPresent(payload, "paymentMode", blankToNull(request.paymentMode()));
		putIfPresent(payload, "examTypeId", request.examTypeId());
		putIfPresent(payload, "examScheduleId", request.examScheduleId());
		putIfPresent(payload, "subjectId", request.subjectId());
		putIfPresent(payload, "studentId", request.studentId());
		putIfPresent(payload, "hostelId", request.hostelId());
		putIfPresent(payload, "roomId", request.roomId());
		putIfPresent(payload, "routeId", request.routeId());
		putIfPresent(payload, "vehicleId", request.vehicleId());
		putIfPresent(payload, "departmentId", request.departmentId());
		putIfPresent(payload, "designationId", request.designationId());
		putIfPresent(payload, "staffId", request.staffId());
		putIfPresent(payload, "staffType", blankToNull(request.staffType()));
		putIfPresent(payload, "payrollYear", request.payrollYear());
		putIfPresent(payload, "payrollMonth", request.payrollMonth());
		putIfPresent(payload, "libraryCategoryId", request.libraryCategoryId());
		putIfPresent(payload, "libraryPublisherId", request.libraryPublisherId());
		putIfPresent(payload, "libraryBookId", request.libraryBookId());
		putIfPresent(payload, "libraryMembershipId", request.libraryMembershipId());
		putIfPresent(payload, "memberType", blankToNull(request.memberType()));
		putIfPresent(payload, "libraryKeyword", blankToNull(request.libraryKeyword()));
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
			String status,
			String paymentMode,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId,
			UUID hostelId,
			UUID roomId,
			UUID routeId,
			UUID vehicleId,
			String module,
			String action,
			String performedBy,
			UUID departmentId,
			UUID designationId,
			UUID staffId,
			String staffType,
			Integer payrollYear,
			Integer payrollMonth,
			UUID libraryCategoryId,
			UUID libraryPublisherId,
			UUID libraryBookId,
			UUID libraryMembershipId,
			String memberType,
			String libraryKeyword) {

		public ReportExportRequest(
				String reportType,
				String format,
				UUID academicYearId,
				UUID classId,
				UUID sectionId,
				LocalDate fromDate,
				LocalDate toDate,
				String status,
				String paymentMode,
				UUID examTypeId,
				UUID examScheduleId,
				UUID subjectId,
				UUID studentId,
				UUID hostelId,
				UUID roomId,
				UUID routeId,
				UUID vehicleId,
				String module,
				String action,
				String performedBy,
				UUID departmentId,
				UUID designationId,
				UUID staffId,
				String staffType,
				Integer payrollYear,
				Integer payrollMonth) {
			this(
					reportType,
					format,
					academicYearId,
					classId,
					sectionId,
					fromDate,
					toDate,
					status,
					paymentMode,
					examTypeId,
					examScheduleId,
					subjectId,
					studentId,
					hostelId,
					roomId,
					routeId,
					vehicleId,
					module,
					action,
					performedBy,
					departmentId,
					designationId,
					staffId,
					staffType,
					payrollYear,
					payrollMonth,
					null,
					null,
					null,
					null,
					null,
					null);
		}
	}

	public record ReportExportFile(byte[] content, String filename, String contentType) {
	}
}
