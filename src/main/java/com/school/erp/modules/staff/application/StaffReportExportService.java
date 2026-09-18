package com.school.erp.modules.staff.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.importexport.CsvExportService;
import com.school.erp.common.importexport.ExcelExportService;
import com.school.erp.common.importexport.PdfExportService;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.StaffRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffReportExportService {

	public static final List<String> STAFF_LIST_HEADERS = List.of(
			"Employee Code",
			"Staff Name",
			"Type",
			"Department",
			"Designation",
			"Email",
			"Phone",
			"Joining Date",
			"Status");
	public static final List<String> STAFF_ATTENDANCE_HEADERS = List.of(
			"Date",
			"Employee Code",
			"Staff Name",
			"Department",
			"Designation",
			"Status",
			"Remarks");
	public static final List<String> STAFF_LEAVE_HEADERS = List.of(
			"Employee Code",
			"Staff Name",
			"Leave Type",
			"Start Date",
			"End Date",
			"Duration Days",
			"Status",
			"Requested By",
			"Reviewed By");
	public static final List<String> STAFF_PAYROLL_HEADERS = List.of(
			"Employee Code",
			"Staff Name",
			"Period",
			"Salary Structure",
			"Basic",
			"Allowances",
			"Deductions",
			"Gross",
			"Net",
			"Status");

	private final StaffRepository staffRepository;
	private final StaffAttendanceService staffAttendanceService;
	private final StaffLeaveService staffLeaveService;
	private final PayrollService payrollService;
	private final ExcelExportService excelExportService;
	private final CsvExportService csvExportService;
	private final PdfExportService pdfExportService;

	@Transactional(readOnly = true)
	public byte[] staffListReport(
			String format,
			EmploymentStatus status,
			StaffType staffType,
			UUID departmentId,
			UUID designationId) {
		requireAuthority("STAFF_READ");
		return export(
				format,
				"Staff List Report",
				"staff-list-report",
				STAFF_LIST_HEADERS,
				staffListRows(status, staffType, departmentId, designationId),
				filters(status, staffType, departmentId, designationId, null, null, null, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> staffListRows(
			EmploymentStatus status,
			StaffType staffType,
			UUID departmentId,
			UUID designationId) {
		requireAuthority("STAFF_READ");
		List<Map<String, Object>> rows = new ArrayList<>();
		int pageNumber = 0;
		Page<Staff> page;
		do {
			page = staffRepository.search(
					status,
					staffType,
					departmentId,
					designationId,
					PageRequest.of(pageNumber, 200, Sort.by("firstName").ascending()));
			page.getContent().stream()
					.map(this::staffRow)
					.forEach(rows::add);
			pageNumber++;
		}
		while (page.hasNext());
		return rows;
	}

	@Transactional(readOnly = true)
	public byte[] staffAttendanceReport(
			String format,
			LocalDate fromDate,
			LocalDate toDate,
			UUID departmentId,
			UUID designationId) {
		requireAuthority("ATTENDANCE_READ");
		return export(
				format,
				"Staff Attendance Report",
				"staff-attendance-report",
				STAFF_ATTENDANCE_HEADERS,
				staffAttendanceRows(fromDate, toDate, departmentId, designationId),
				filters(null, null, departmentId, designationId, null, fromDate, toDate, null, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> staffAttendanceRows(
			LocalDate fromDate,
			LocalDate toDate,
			UUID departmentId,
			UUID designationId) {
		requireAuthority("ATTENDANCE_READ");
		return staffAttendanceService.reportRows(fromDate, toDate, departmentId, designationId);
	}

	@Transactional(readOnly = true)
	public byte[] staffLeaveReport(
			String format,
			UUID staffId,
			LeaveStatus status,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority("LEAVE_READ");
		return export(
				format,
				"Staff Leave Report",
				"staff-leave-report",
				STAFF_LEAVE_HEADERS,
				staffLeaveRows(staffId, status, fromDate, toDate),
				filters(null, null, null, null, staffId, fromDate, toDate, status, null));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> staffLeaveRows(
			UUID staffId,
			LeaveStatus status,
			LocalDate fromDate,
			LocalDate toDate) {
		requireAuthority("LEAVE_READ");
		return staffLeaveService.reportRows(staffId, status, fromDate, toDate);
	}

	@Transactional(readOnly = true)
	public byte[] staffPayrollReport(
			String format,
			UUID staffId,
			Integer payrollYear,
			Integer payrollMonth,
			PayrollStatus status) {
		requireAuthority("PAYROLL_READ");
		return export(
				format,
				"Staff Payroll Report",
				"staff-payroll-report",
				STAFF_PAYROLL_HEADERS,
				staffPayrollRows(staffId, payrollYear, payrollMonth, status),
				filters(null, null, null, null, staffId, null, null, null, status, payrollYear, payrollMonth));
	}

	@Transactional(readOnly = true)
	public List<Map<String, Object>> staffPayrollRows(
			UUID staffId,
			Integer payrollYear,
			Integer payrollMonth,
			PayrollStatus status) {
		requireAuthority("PAYROLL_READ");
		return payrollService.reportRows(staffId, payrollYear, payrollMonth, status);
	}

	private Map<String, Object> staffRow(Staff staff) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Employee Code", staff.getEmployeeCode());
		row.put("Staff Name", staff.getDisplayName());
		row.put("Type", staff.getStaffType());
		row.put("Department", staff.getDepartment() == null ? null : staff.getDepartment().getName());
		row.put("Designation", staff.getDesignation() == null ? null : staff.getDesignation().getName());
		row.put("Email", staff.getEmail());
		row.put("Phone", staff.getPhoneNumber());
		row.put("Joining Date", staff.getJoiningDate());
		row.put("Status", staff.getStatus());
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
			EmploymentStatus employmentStatus,
			StaffType staffType,
			UUID departmentId,
			UUID designationId,
			UUID staffId,
			LocalDate fromDate,
			LocalDate toDate,
			LeaveStatus leaveStatus,
			PayrollStatus payrollStatus) {
		return filters(
				employmentStatus,
				staffType,
				departmentId,
				designationId,
				staffId,
				fromDate,
				toDate,
				leaveStatus,
				payrollStatus,
				null,
				null);
	}

	private Map<String, Object> filters(
			EmploymentStatus employmentStatus,
			StaffType staffType,
			UUID departmentId,
			UUID designationId,
			UUID staffId,
			LocalDate fromDate,
			LocalDate toDate,
			LeaveStatus leaveStatus,
			PayrollStatus payrollStatus,
			Integer payrollYear,
			Integer payrollMonth) {
		Map<String, Object> filters = new LinkedHashMap<>();
		filters.put("Employment Status", employmentStatus);
		filters.put("Staff Type", staffType);
		filters.put("Department ID", departmentId);
		filters.put("Designation ID", designationId);
		filters.put("Staff ID", staffId);
		filters.put("From Date", fromDate);
		filters.put("To Date", toDate);
		filters.put("Leave Status", leaveStatus);
		filters.put("Payroll Status", payrollStatus);
		filters.put("Payroll Year", payrollYear);
		filters.put("Payroll Month", payrollMonth);
		return filters;
	}

	private String normalizeFormat(String format) {
		return format == null ? "EXCEL" : format.trim().toUpperCase(Locale.ROOT);
	}

	private void requireAuthority(String authority) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		boolean allowed = authentication.getAuthorities().stream()
				.anyMatch(granted -> granted.getAuthority().equals(authority));
		if (!allowed) {
			throw new BusinessException(ErrorCode.FORBIDDEN, authority + " permission is required.");
		}
	}
}
