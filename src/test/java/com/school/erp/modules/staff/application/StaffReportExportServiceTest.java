package com.school.erp.modules.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.Gender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffReportExportServiceTest {

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private StaffAttendanceService staffAttendanceService;

	@Mock
	private StaffLeaveService staffLeaveService;

	@Mock
	private PayrollService payrollService;

	@Mock
	private ExcelExportService excelExportService;

	@Mock
	private CsvExportService csvExportService;

	@Mock
	private PdfExportService pdfExportService;

	private StaffReportExportService reportExportService;
	private Department department;
	private Designation designation;

	@BeforeEach
	void setUp() {
		reportExportService = new StaffReportExportService(
				staffRepository,
				staffAttendanceService,
				staffLeaveService,
				payrollService,
				excelExportService,
				csvExportService,
				pdfExportService);
		department = new Department("Administration", null, true);
		designation = new Designation("Coordinator", department, null, true);
		setId(department);
		setId(designation);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void staffListRowsWalksRepositoryPagesWithFilters() {
		authenticate("STAFF_READ");
		Staff first = staff("EMP-101", "Asha");
		Staff second = staff("EMP-102", "Bhavna");
		when(staffRepository.search(
				eq(EmploymentStatus.ACTIVE),
				eq(StaffType.NON_TEACHING),
				eq(department.getId()),
				eq(designation.getId()),
				argThat(pageable -> pageable.getPageNumber() == 0)))
				.thenReturn(new PageImpl<>(List.of(first), PageRequest.of(0, 200), 201));
		when(staffRepository.search(
				eq(EmploymentStatus.ACTIVE),
				eq(StaffType.NON_TEACHING),
				eq(department.getId()),
				eq(designation.getId()),
				argThat(pageable -> pageable.getPageNumber() == 1)))
				.thenReturn(new PageImpl<>(List.of(second), PageRequest.of(1, 200), 201));

		var rows = reportExportService.staffListRows(
				EmploymentStatus.ACTIVE,
				StaffType.NON_TEACHING,
				department.getId(),
				designation.getId());

		assertThat(rows).hasSize(2);
		assertThat(rows.getFirst()).containsEntry("Employee Code", "EMP-101");
		assertThat(rows.getLast()).containsEntry("Staff Name", "Bhavna Rao");
	}

	@Test
	void staffAttendancePdfDelegatesToCanonicalRowsAndPdfExporter() {
		authenticate("ATTENDANCE_READ");
		LocalDate fromDate = LocalDate.of(2026, 9, 1);
		LocalDate toDate = LocalDate.of(2026, 9, 30);
		List<Map<String, Object>> rows = List.of(Map.of(
				"Date", fromDate,
				"Employee Code", "EMP-101",
				"Status", AttendanceStatus.PRESENT));
		when(staffAttendanceService.reportRows(fromDate, toDate, department.getId(), null)).thenReturn(rows);
		when(pdfExportService.exportTable(
				eq("Staff Attendance Report"),
				anyMap(),
				eq(StaffReportExportService.STAFF_ATTENDANCE_HEADERS),
				eq(rows)))
				.thenReturn(bytes("attendance-pdf"));

		var content = reportExportService.staffAttendanceReport("PDF", fromDate, toDate, department.getId(), null);

		assertThat(content).isEqualTo(bytes("attendance-pdf"));
	}

	@Test
	void staffLeaveCsvDelegatesToLeaveRowsAndCsvExporter() {
		authenticate("LEAVE_READ");
		UUID staffId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(Map.of(
				"Employee Code", "EMP-101",
				"Status", LeaveStatus.APPROVED));
		when(staffLeaveService.reportRows(staffId, LeaveStatus.APPROVED, null, null)).thenReturn(rows);
		when(csvExportService.export(StaffReportExportService.STAFF_LEAVE_HEADERS, rows)).thenReturn(bytes("leave-csv"));

		var content = reportExportService.staffLeaveReport("CSV", staffId, LeaveStatus.APPROVED, null, null);

		assertThat(content).isEqualTo(bytes("leave-csv"));
	}

	@Test
	void staffPayrollExcelDelegatesToPayrollRowsAndExcelExporter() {
		authenticate("PAYROLL_READ");
		UUID staffId = UUID.randomUUID();
		List<Map<String, Object>> rows = List.of(Map.of(
				"Employee Code", "EMP-101",
				"Status", PayrollStatus.PAID));
		when(payrollService.reportRows(staffId, 2026, 9, PayrollStatus.PAID)).thenReturn(rows);
		when(excelExportService.export("staff-payroll-report", StaffReportExportService.STAFF_PAYROLL_HEADERS, rows))
				.thenReturn(bytes("payroll-xlsx"));

		var content = reportExportService.staffPayrollReport("EXCEL", staffId, 2026, 9, PayrollStatus.PAID);

		assertThat(content).isEqualTo(bytes("payroll-xlsx"));
	}

	@Test
	void staffPayrollReportRequiresPayrollReadAuthority() {
		authenticate("STAFF_READ");

		assertThatThrownBy(() -> reportExportService.staffPayrollReport("CSV", null, 2026, 9, PayrollStatus.PENDING))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private Staff staff(String employeeCode, String firstName) {
		Staff staff = new Staff(
				employeeCode,
				firstName,
				null,
				"Rao",
				Gender.FEMALE,
				LocalDate.of(1990, 5, 10),
				firstName.toLowerCase() + "@school.test",
				"9890000001",
				null,
				null,
				department,
				designation,
				LocalDate.of(2024, 6, 1),
				StaffType.NON_TEACHING,
				EmploymentStatus.ACTIVE);
		setId(staff);
		return staff;
	}

	private byte[] bytes(String value) {
		return value.getBytes(StandardCharsets.UTF_8);
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"reports-user",
				null,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
