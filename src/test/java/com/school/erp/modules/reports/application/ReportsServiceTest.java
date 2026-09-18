package com.school.erp.modules.reports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogExportService;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.api.dto.ClassResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.exams.application.ExamReportExportService;
import com.school.erp.modules.fees.api.dto.FeeReportRequest;
import com.school.erp.modules.fees.application.FeeImportExportService;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.hostel.application.HostelReportExportService;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.library.application.LibraryReportExportService;
import com.school.erp.modules.library.domain.LibraryFineStatus;
import com.school.erp.modules.reports.application.ReportsService.ReportExportRequest;
import com.school.erp.modules.staff.application.StaffReportExportService;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.application.StudentImportExportService;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.transport.application.TransportReportExportService;
import com.school.erp.modules.transport.domain.TransportStatus;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReportsServiceTest {

	@Mock
	private StudentImportExportService studentImportExportService;

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private AttendanceService attendanceService;

	@Mock
	private FeeImportExportService feeImportExportService;

	@Mock
	private ExamReportExportService examReportExportService;

	@Mock
	private HostelReportExportService hostelReportExportService;

	@Mock
	private TransportReportExportService transportReportExportService;

	@Mock
	private StaffReportExportService staffReportExportService;

	@Mock
	private LibraryReportExportService libraryReportExportService;

	@Mock
	private AuditLogExportService auditLogExportService;

	@Mock
	private AuditLogService auditLogService;

	@Mock
	private ExcelExportService excelExportService;

	@Mock
	private CsvExportService csvExportService;

	@Mock
	private PdfExportService pdfExportService;

	private ReportsService reportsService;

	@BeforeEach
	void setUp() {
		reportsService = new ReportsService(
				studentImportExportService,
				academicHierarchyService,
				attendanceService,
				feeImportExportService,
				examReportExportService,
				hostelReportExportService,
				transportReportExportService,
				staffReportExportService,
				libraryReportExportService,
				auditLogExportService,
				auditLogService,
				excelExportService,
				csvExportService,
				pdfExportService);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void optionsExposeCanonicalReportFamiliesAndFormats() {
		var options = reportsService.options();

		assertThat(options.reportTypes())
				.extracting("code")
				.contains(
						"STUDENT_REPORT",
						"ACADEMIC_STRUCTURE_REPORT",
						"ATTENDANCE_REPORT",
						"STAFF_LIST_REPORT",
						"STAFF_ATTENDANCE_REPORT",
						"STAFF_LEAVE_REPORT",
						"STAFF_PAYROLL_REPORT",
						"EXAM_SCHEDULE_REPORT",
						"EXAM_MARKS_REPORT",
						"EXAM_RESULT_REPORT",
						"FEE_COLLECTION_REPORT",
						"DEFAULTER_REPORT",
						"HOSTEL_OCCUPANCY_REPORT",
						"TRANSPORT_ASSIGNMENT_REPORT");
		assertThat(options.reportTypes())
				.extracting("code")
				.contains(
						"LIBRARY_INVENTORY_REPORT",
						"LIBRARY_AVAILABLE_BOOKS_REPORT",
						"LIBRARY_ISSUED_BOOKS_REPORT",
						"LIBRARY_OVERDUE_BOOKS_REPORT",
						"LIBRARY_FINE_REPORT");
		var studentReport = options.reportTypes().stream()
				.filter(report -> report.code().equals("STUDENT_REPORT"))
				.findFirst()
				.orElseThrow();
		assertThat(studentReport.formats()).containsExactly("EXCEL", "CSV", "PDF");
	}

	@Test
	void studentPdfUsesFilteredStudentExport() {
		UUID academicYearId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		when(studentImportExportService.exportPdf(any(StudentSearchRequest.class))).thenReturn(bytes("students"));

		var file = reportsService.export(request(
				"STUDENT_REPORT",
				"PDF",
				academicYearId,
				classId,
				sectionId,
				null,
				null,
				"ACTIVE",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("students.pdf");
		assertThat(file.contentType()).isEqualTo("application/pdf");
		verify(studentImportExportService).exportPdf(argThat(search ->
				search.academicYearId().equals(academicYearId)
						&& search.classId().equals(classId)
						&& search.sectionId().equals(sectionId)
						&& search.status() == StudentStatus.ACTIVE));
	}

	@Test
	void academicStructurePdfUsesAcademicHierarchyRows() {
		UUID academicYearId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		AcademicYearResponse academicYear = new AcademicYearResponse(
				academicYearId,
				"AY-2026-27",
				"2026-2027",
				LocalDate.of(2026, 4, 1),
				LocalDate.of(2027, 3, 31),
				true,
				true,
				null);
		ClassResponse classResponse = new ClassResponse(classId, academicYearId, "CLASS-6", "Class 6", 6, true);
		SectionEntity section = section(classEntity(academicYear()));
		when(academicHierarchyService.getClass(classId)).thenReturn(classResponse);
		when(academicHierarchyService.getAcademicYear(academicYearId)).thenReturn(academicYear);
		when(academicHierarchyService.loadSectionForClass(classId, sectionId)).thenReturn(section);
		when(pdfExportService.exportTable(eq("Academic Structure Report"), anyMap(), any(), any()))
				.thenReturn(bytes("academic"));

		var file = reportsService.export(request(
				"ACADEMIC_STRUCTURE_REPORT",
				"PDF",
				academicYearId,
				classId,
				sectionId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("academic-structure.pdf");
		assertThat(file.content()).isEqualTo(bytes("academic"));
		verify(pdfExportService).exportTable(eq("Academic Structure Report"), anyMap(), any(), argThat(rows ->
				rows.size() == 2
						&& "Class".equals(rows.get(0).get("Level"))
						&& "Section".equals(rows.get(1).get("Level"))
						&& "2026-2027".equals(rows.get(1).get("Academic Year"))
						&& "Class 6".equals(rows.get(1).get("Class"))
						&& "Division A".equals(rows.get(1).get("Section"))));
	}

	@Test
	void attendancePdfUsesCanonicalRowsAndPdfExport() {
		UUID academicYearId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		LocalDate fromDate = LocalDate.of(2026, 6, 1);
		LocalDate toDate = LocalDate.of(2026, 6, 30);
		List<Map<String, Object>> rows = List.of(row("Date", fromDate, "Status", "PRESENT"));
		when(attendanceService.reportRows(academicYearId, classId, sectionId, fromDate, toDate)).thenReturn(rows);
		when(pdfExportService.exportTable(
				eq("Attendance Report"),
				anyMap(),
				eq(AttendanceService.ATTENDANCE_REPORT_HEADERS),
				eq(rows)))
				.thenReturn(bytes("attendance-pdf"));

		var file = reportsService.export(request(
				"ATTENDANCE_REPORT",
				"PDF",
				academicYearId,
				classId,
				sectionId,
				fromDate,
				toDate,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("attendance.pdf");
		assertThat(file.content()).isEqualTo(bytes("attendance-pdf"));
		verify(attendanceService).reportRows(academicYearId, classId, sectionId, fromDate, toDate);
	}

	@Test
	void staffListPdfDelegatesToStaffReportExporterWithFilters() {
		UUID departmentId = UUID.randomUUID();
		UUID designationId = UUID.randomUUID();
		when(staffReportExportService.staffListReport(
				"PDF",
				EmploymentStatus.ACTIVE,
				StaffType.NON_TEACHING,
				departmentId,
				designationId))
				.thenReturn(bytes("staff-list"));

		var file = reportsService.export(staffRequest(
				"STAFF_LIST_REPORT",
				"PDF",
				"ACTIVE",
				"NON_TEACHING",
				departmentId,
				designationId,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("staff-list.pdf");
		assertThat(file.content()).isEqualTo(bytes("staff-list"));
		verify(staffReportExportService).staffListReport(
				"PDF",
				EmploymentStatus.ACTIVE,
				StaffType.NON_TEACHING,
				departmentId,
				designationId);
	}

	@Test
	void staffAttendanceExcelRequiresDateRangeAndDelegatesToStaffExporter() {
		UUID departmentId = UUID.randomUUID();
		LocalDate fromDate = LocalDate.of(2026, 7, 1);
		LocalDate toDate = LocalDate.of(2026, 7, 31);
		when(staffReportExportService.staffAttendanceReport("EXCEL", fromDate, toDate, departmentId, null))
				.thenReturn(bytes("staff-attendance"));

		var file = reportsService.export(staffRequest(
				"STAFF_ATTENDANCE_REPORT",
				"EXCEL",
				null,
				null,
				departmentId,
				null,
				null,
				fromDate,
				toDate));

		assertThat(file.filename()).isEqualTo("staff-attendance.xlsx");
		verify(staffReportExportService).staffAttendanceReport("EXCEL", fromDate, toDate, departmentId, null);
	}

	@Test
	void staffPayrollCsvDelegatesWithPeriodAndStatusFilters() {
		UUID staffId = UUID.randomUUID();
		when(staffReportExportService.staffPayrollReport("CSV", staffId, 2026, 8, PayrollStatus.PAID))
				.thenReturn(bytes("payroll"));

		var file = reportsService.export(new ReportExportRequest(
				"STAFF_PAYROLL_REPORT",
				"CSV",
				null,
				null,
				null,
				null,
				null,
				"PAID",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				staffId,
				null,
				2026,
				8));

		assertThat(file.filename()).isEqualTo("staff-payroll.csv");
		assertThat(file.contentType()).isEqualTo("text/csv");
	}

	@Test
	void staffLeavePreviewPaginatesRowsFromStaffExporter() {
		UUID staffId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(
				row("Employee Code", "EMP-001", "Status", "PENDING"),
				row("Employee Code", "EMP-002", "Status", "APPROVED"));
		when(staffReportExportService.staffLeaveRows(staffId, null, null, null)).thenReturn(rows);

		var page = reportsService.preview(new ReportExportRequest(
				"STAFF_LEAVE_REPORT",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				staffId,
				null,
				null,
				null), new com.school.erp.common.api.PageRequestDto(1, 1, null, null));

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst()).containsEntry("Employee Code", "EMP-002");
		assertThat(page.totalElements()).isEqualTo(2);
	}

	@Test
	void feeCollectionExcelForwardsCanonicalAcademicFilters() {
		AcademicYear academicYear = academicYear();
		ClassEntity classEntity = classEntity(academicYear);
		SectionEntity section = section(classEntity);
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
		when(feeImportExportService.collectionReport(eq("EXCEL"), any(FeeReportRequest.class))).thenReturn(bytes("fees"));

		var file = reportsService.export(request(
				"FEE_COLLECTION_REPORT",
				"EXCEL",
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				null,
				null,
				"PENDING",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("fee-collection-summary.xlsx");
		verify(feeImportExportService).collectionReport(eq("EXCEL"), argThat(filter ->
				filter.academicYear().equals("2026-2027")
						&& filter.className().equals("Class 6")
						&& filter.sectionName().equals("Division A")
						&& filter.status() == FeeAssignmentStatus.PENDING));
	}

	@Test
	void examResultPdfDelegatesToCanonicalExamExporter() {
		UUID academicYearId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		UUID sectionId = UUID.randomUUID();
		UUID examTypeId = UUID.randomUUID();
		UUID examScheduleId = UUID.randomUUID();
		UUID subjectId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		when(examReportExportService.resultReport(
				"PDF",
				academicYearId,
				classId,
				sectionId,
				examTypeId,
				examScheduleId,
				subjectId,
				studentId))
				.thenReturn(bytes("exam"));

		var file = reportsService.export(request(
				"EXAM_RESULT_REPORT",
				"PDF",
				academicYearId,
				classId,
				sectionId,
				null,
				null,
				null,
				examTypeId,
				examScheduleId,
				subjectId,
				studentId,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("exam-results.pdf");
		assertThat(file.content()).isEqualTo(bytes("exam"));
	}

	@Test
	void hostelOccupancyExcelDelegatesToCanonicalHostelExporter() {
		UUID academicYearId = UUID.randomUUID();
		UUID hostelId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		when(hostelReportExportService.occupancyReport("EXCEL", academicYearId, hostelId, roomId))
				.thenReturn(bytes("hostel"));

		var file = reportsService.export(request(
				"HOSTEL_OCCUPANCY_REPORT",
				"EXCEL",
				academicYearId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				hostelId,
				roomId,
				null,
				null));

		assertThat(file.filename()).isEqualTo("hostel-occupancy.xlsx");
		assertThat(file.content()).isEqualTo(bytes("hostel"));
	}

	@Test
	void transportAssignmentCsvDelegatesToCanonicalTransportExporter() {
		UUID academicYearId = UUID.randomUUID();
		UUID vehicleId = UUID.randomUUID();
		UUID routeId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		when(transportReportExportService.assignmentsReport(
				"CSV",
				academicYearId,
				vehicleId,
				routeId,
				studentId,
				TransportStatus.ASSIGNED))
				.thenReturn(bytes("transport"));

		var file = reportsService.export(request(
				"TRANSPORT_ASSIGNMENT_REPORT",
				"CSV",
				academicYearId,
				null,
				null,
				null,
				null,
				"ASSIGNED",
				null,
				null,
				null,
				studentId,
				null,
				null,
				routeId,
				vehicleId));

		assertThat(file.filename()).isEqualTo("transport-assignments.csv");
		assertThat(file.contentType()).isEqualTo("text/csv");
	}

	@Test
	void libraryInventoryPdfDelegatesToLibraryExporterWithFilters() {
		UUID categoryId = UUID.randomUUID();
		UUID publisherId = UUID.randomUUID();
		when(libraryReportExportService.inventoryReport("PDF", categoryId, publisherId, true))
				.thenReturn(bytes("library-inventory"));

		var file = reportsService.export(libraryRequest(
				"LIBRARY_INVENTORY_REPORT",
				"PDF",
				"ACTIVE",
				categoryId,
				publisherId,
				null,
				null,
				null,
				null,
				null,
				null));

		assertThat(file.filename()).isEqualTo("library-inventory.pdf");
		assertThat(file.content()).isEqualTo(bytes("library-inventory"));
	}

	@Test
	void libraryFinePreviewPaginatesRowsFromLibraryExporter() {
		UUID membershipId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(
				row("Membership Number", "LIB-M-001", "Status", "PENDING"),
				row("Membership Number", "LIB-M-002", "Status", "PAID"));
		when(libraryReportExportService.fineRows(LibraryFineStatus.PENDING, membershipId, null, null)).thenReturn(rows);

		var page = reportsService.preview(libraryRequest(
				"LIBRARY_FINE_REPORT",
				null,
				"PENDING",
				null,
				null,
				null,
				membershipId,
				null,
				null,
				null,
				null), new com.school.erp.common.api.PageRequestDto(1, 1, null, null));

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst()).containsEntry("Membership Number", "LIB-M-002");
		assertThat(page.totalElements()).isEqualTo(2);
	}

	@Test
	void previewPaginatesFilteredStudentRowsWithoutLimitingExportDataset() {
		UUID academicYearId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(
				row("admissionNumber", "ADM-001", "studentName", "Aarav"),
				row("admissionNumber", "ADM-002", "studentName", "Diya"));
		when(studentImportExportService.reportRows(any(StudentSearchRequest.class))).thenReturn(rows);

		var page = reportsService.preview(
				request(
						"STUDENT_REPORT",
						null,
						academicYearId,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null),
				new com.school.erp.common.api.PageRequestDto(1, 1, null, null));

		assertThat(page.content()).hasSize(1);
		assertThat(page.content().getFirst()).containsEntry("admissionNumber", "ADM-002");
		assertThat(page.totalElements()).isEqualTo(2);
		assertThat(page.totalPages()).isEqualTo(2);
		verify(studentImportExportService).reportRows(argThat(search -> search.academicYearId().equals(academicYearId)));
	}

	@Test
	void marksReportRequiresScheduleAndSubjectFilters() {
		assertThatThrownBy(() -> reportsService.export(request(
				"EXAM_MARKS_REPORT",
				"EXCEL",
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void reportAuthorizationRequiresReportAndModulePermission() {
		authenticate("REPORTS_READ");

		assertThatThrownBy(() -> reportsService.export(request(
				"FEE_COLLECTION_REPORT",
				"CSV",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void staffReportAuthorizationRequiresStaffPermission() {
		authenticate("REPORTS_READ");

		assertThatThrownBy(() -> reportsService.export(staffRequest(
				"STAFF_LIST_REPORT",
				"CSV",
				null,
				null,
				null,
				null,
				null,
				null,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void libraryReportAuthorizationRequiresLibraryPermission() {
		authenticate("REPORTS_READ");

		assertThatThrownBy(() -> reportsService.export(libraryRequest(
				"LIBRARY_AVAILABLE_BOOKS_REPORT",
				"CSV",
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private ReportExportRequest request(
			String reportType,
			String format,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			LocalDate fromDate,
			LocalDate toDate,
			String status,
			UUID examTypeId,
			UUID examScheduleId,
			UUID subjectId,
			UUID studentId,
			UUID hostelId,
			UUID roomId,
			UUID routeId,
			UUID vehicleId) {
		return new ReportExportRequest(
				reportType,
				format,
				academicYearId,
				classId,
				sectionId,
				fromDate,
				toDate,
				status,
				null,
				examTypeId,
				examScheduleId,
				subjectId,
				studentId,
				hostelId,
				roomId,
				routeId,
				vehicleId,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private ReportExportRequest staffRequest(
			String reportType,
			String format,
			String status,
			String staffType,
			UUID departmentId,
			UUID designationId,
			UUID staffId,
			LocalDate fromDate,
			LocalDate toDate) {
		return new ReportExportRequest(
				reportType,
				format,
				null,
				null,
				null,
				fromDate,
				toDate,
				status,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				departmentId,
				designationId,
				staffId,
				staffType,
				null,
				null);
	}

	private ReportExportRequest libraryRequest(
			String reportType,
			String format,
			String status,
			UUID categoryId,
			UUID publisherId,
			UUID bookId,
			UUID membershipId,
			String memberType,
			String keyword,
			LocalDate fromDate,
			LocalDate toDate) {
		return new ReportExportRequest(
				reportType,
				format,
				null,
				null,
				null,
				fromDate,
				toDate,
				status,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				categoryId,
				publisherId,
				bookId,
				membershipId,
				memberType,
				keyword);
	}

	private Map<String, Object> row(String key1, Object value1, String key2, Object value2) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put(key1, value1);
		row.put(key2, value2);
		return row;
	}

	private AcademicYear academicYear() {
		AcademicYear academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		setId(academicYear);
		return academicYear;
	}

	private ClassEntity classEntity(AcademicYear academicYear) {
		ClassEntity classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		setId(classEntity);
		return classEntity;
	}

	private SectionEntity section(ClassEntity classEntity) {
		SectionEntity section = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		setId(section);
		return section;
	}

	private byte[] bytes(String value) {
		return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"reports-user",
				null,
				List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
