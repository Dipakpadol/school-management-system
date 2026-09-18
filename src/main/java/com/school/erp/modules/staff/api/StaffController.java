package com.school.erp.modules.staff.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.staff.api.dto.DepartmentRequest;
import com.school.erp.modules.staff.api.dto.DepartmentResponse;
import com.school.erp.modules.staff.api.dto.DesignationRequest;
import com.school.erp.modules.staff.api.dto.DesignationResponse;
import com.school.erp.modules.staff.api.dto.LeaveTypeRequest;
import com.school.erp.modules.staff.api.dto.LeaveTypeResponse;
import com.school.erp.modules.staff.api.dto.PayrollGenerateRequest;
import com.school.erp.modules.staff.api.dto.PayrollRecordResponse;
import com.school.erp.modules.staff.api.dto.SalaryStructureRequest;
import com.school.erp.modules.staff.api.dto.SalaryStructureResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceDailyRequest;
import com.school.erp.modules.staff.api.dto.StaffAttendanceDailyResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceRecordResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceSummaryResponse;
import com.school.erp.modules.staff.api.dto.StaffDocumentRequest;
import com.school.erp.modules.staff.api.dto.StaffDocumentResponse;
import com.school.erp.modules.staff.api.dto.StaffExitRequest;
import com.school.erp.modules.staff.api.dto.StaffLeaveCreateRequest;
import com.school.erp.modules.staff.api.dto.StaffLeaveResponse;
import com.school.erp.modules.staff.api.dto.StaffLeaveReviewRequest;
import com.school.erp.modules.staff.api.dto.StaffRequest;
import com.school.erp.modules.staff.api.dto.StaffResponse;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentRequest;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentResponse;
import com.school.erp.modules.staff.application.PayrollService;
import com.school.erp.modules.staff.application.StaffAttendanceService;
import com.school.erp.modules.staff.application.StaffLeaveService;
import com.school.erp.modules.staff.application.StaffService;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.StaffType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff", description = "Canonical staff, leave, payroll, documents, and staff attendance.")
public class StaffController {

	private final StaffService staffService;
	private final StaffAttendanceService staffAttendanceService;
	private final StaffLeaveService staffLeaveService;
	private final PayrollService payrollService;

	@GetMapping("/departments")
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ResponseEntity<ApiResponse<List<DepartmentResponse>>> departments(HttpServletRequest request) {
		return ok(staffService.departments(), "Departments fetched successfully", request);
	}

	@PostMapping("/departments")
	@PreAuthorize("hasAuthority('STAFF_CREATE')")
	public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
			@Valid @RequestBody DepartmentRequest body,
			HttpServletRequest request) {
		return created(staffService.createDepartment(body), "Department created successfully", request);
	}

	@PutMapping("/departments/{departmentId}")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
			@PathVariable UUID departmentId,
			@Valid @RequestBody DepartmentRequest body,
			HttpServletRequest request) {
		return ok(staffService.updateDepartment(departmentId, body), "Department updated successfully", request);
	}

	@GetMapping("/designations")
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ResponseEntity<ApiResponse<List<DesignationResponse>>> designations(HttpServletRequest request) {
		return ok(staffService.designations(), "Designations fetched successfully", request);
	}

	@PostMapping("/designations")
	@PreAuthorize("hasAuthority('STAFF_CREATE')")
	public ResponseEntity<ApiResponse<DesignationResponse>> createDesignation(
			@Valid @RequestBody DesignationRequest body,
			HttpServletRequest request) {
		return created(staffService.createDesignation(body), "Designation created successfully", request);
	}

	@PutMapping("/designations/{designationId}")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<DesignationResponse>> updateDesignation(
			@PathVariable UUID designationId,
			@Valid @RequestBody DesignationRequest body,
			HttpServletRequest request) {
		return ok(staffService.updateDesignation(designationId, body), "Designation updated successfully", request);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ResponseEntity<ApiResponse<PageResponse<StaffResponse>>> staff(
			@RequestParam(required = false) EmploymentStatus status,
			@RequestParam(required = false) StaffType staffType,
			@RequestParam(required = false) UUID departmentId,
			@RequestParam(required = false) UUID designationId,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				staffService.staff(status, staffType, departmentId, designationId, pageRequest),
				"Staff fetched successfully",
				request);
	}

	@PostMapping
	@PreAuthorize("hasAuthority('STAFF_CREATE')")
	public ResponseEntity<ApiResponse<StaffResponse>> createStaff(
			@Valid @RequestBody StaffRequest body,
			HttpServletRequest request) {
		return created(staffService.createStaff(body), "Staff created successfully", request);
	}

	@GetMapping("/{staffId}")
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ResponseEntity<ApiResponse<StaffResponse>> getStaff(
			@PathVariable UUID staffId,
			HttpServletRequest request) {
		return ok(staffService.getStaff(staffId), "Staff fetched successfully", request);
	}

	@PutMapping("/{staffId}")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
			@PathVariable UUID staffId,
			@Valid @RequestBody StaffRequest body,
			HttpServletRequest request) {
		return ok(staffService.updateStaff(staffId, body), "Staff updated successfully", request);
	}

	@PatchMapping("/{staffId}/deactivate")
	@PreAuthorize("hasAuthority('STAFF_DELETE')")
	public ResponseEntity<ApiResponse<StaffResponse>> deactivateStaff(
			@PathVariable UUID staffId,
			HttpServletRequest request) {
		return ok(staffService.deactivateStaff(staffId), "Staff deactivated successfully", request);
	}

	@PatchMapping("/{staffId}/exit")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<StaffResponse>> exitStaff(
			@PathVariable UUID staffId,
			@Valid @RequestBody StaffExitRequest body,
			HttpServletRequest request) {
		return ok(staffService.exitStaff(staffId, body), "Staff exit recorded successfully", request);
	}

	@GetMapping("/{staffId}/documents")
	@PreAuthorize("hasAuthority('STAFF_READ')")
	public ResponseEntity<ApiResponse<List<StaffDocumentResponse>>> documents(
			@PathVariable UUID staffId,
			HttpServletRequest request) {
		return ok(staffService.documents(staffId), "Staff documents fetched successfully", request);
	}

	@PostMapping("/{staffId}/documents")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<StaffDocumentResponse>> createDocument(
			@PathVariable UUID staffId,
			@Valid @RequestBody StaffDocumentRequest body,
			HttpServletRequest request) {
		return created(staffService.createDocument(staffId, body), "Staff document saved successfully", request);
	}

	@PutMapping("/{staffId}/documents/{documentId}")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<StaffDocumentResponse>> updateDocument(
			@PathVariable UUID staffId,
			@PathVariable UUID documentId,
			@Valid @RequestBody StaffDocumentRequest body,
			HttpServletRequest request) {
		return ok(staffService.updateDocument(staffId, documentId, body), "Staff document updated successfully", request);
	}

	@PatchMapping("/{staffId}/documents/{documentId}/archive")
	@PreAuthorize("hasAuthority('STAFF_UPDATE')")
	public ResponseEntity<ApiResponse<Void>> deleteDocument(
			@PathVariable UUID staffId,
			@PathVariable UUID documentId,
			HttpServletRequest request) {
		staffService.deleteDocument(staffId, documentId);
		return ok(null, "Staff document archived successfully", request);
	}

	@GetMapping("/attendance/daily")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<StaffAttendanceDailyResponse>> staffAttendanceDaily(
			@RequestParam LocalDate date,
			@RequestParam(required = false) UUID departmentId,
			@RequestParam(required = false) UUID designationId,
			HttpServletRequest request) {
		return ok(
				staffAttendanceService.daily(date, departmentId, designationId),
				"Staff attendance fetched successfully",
				request);
	}

	@PostMapping("/attendance/daily")
	@PreAuthorize("hasAuthority('ATTENDANCE_MARK')")
	public ResponseEntity<ApiResponse<StaffAttendanceDailyResponse>> saveStaffAttendance(
			@Valid @RequestBody StaffAttendanceDailyRequest body,
			HttpServletRequest request) {
		return ok(staffAttendanceService.saveDaily(body), "Staff attendance saved successfully", request);
	}

	@GetMapping("/attendance/summary")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<StaffAttendanceSummaryResponse>> staffAttendanceSummary(
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate,
			@RequestParam(required = false) UUID departmentId,
			@RequestParam(required = false) UUID designationId,
			HttpServletRequest request) {
		return ok(
				staffAttendanceService.summary(fromDate, toDate, departmentId, designationId),
				"Staff attendance summary fetched successfully",
				request);
	}

	@GetMapping("/attendance/monthly")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<StaffAttendanceSummaryResponse>> staffAttendanceMonthly(
			@RequestParam int year,
			@RequestParam int month,
			@RequestParam(required = false) UUID departmentId,
			@RequestParam(required = false) UUID designationId,
			HttpServletRequest request) {
		return ok(
				staffAttendanceService.monthly(year, month, departmentId, designationId),
				"Staff monthly attendance fetched successfully",
				request);
	}

	@GetMapping("/{staffId}/attendance")
	@PreAuthorize("hasAuthority('ATTENDANCE_READ')")
	public ResponseEntity<ApiResponse<List<StaffAttendanceRecordResponse>>> staffAttendanceHistory(
			@PathVariable UUID staffId,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			HttpServletRequest request) {
		return ok(
				staffAttendanceService.history(staffId, fromDate, toDate),
				"Staff attendance history fetched successfully",
				request);
	}

	@GetMapping("/leave-types")
	@PreAuthorize("hasAuthority('LEAVE_READ')")
	public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> leaveTypes(HttpServletRequest request) {
		return ok(staffLeaveService.leaveTypes(), "Leave types fetched successfully", request);
	}

	@PostMapping("/leave-types")
	@PreAuthorize("hasAuthority('LEAVE_CREATE')")
	public ResponseEntity<ApiResponse<LeaveTypeResponse>> createLeaveType(
			@Valid @RequestBody LeaveTypeRequest body,
			HttpServletRequest request) {
		return created(staffLeaveService.createLeaveType(body), "Leave type created successfully", request);
	}

	@PutMapping("/leave-types/{leaveTypeId}")
	@PreAuthorize("hasAuthority('LEAVE_CREATE')")
	public ResponseEntity<ApiResponse<LeaveTypeResponse>> updateLeaveType(
			@PathVariable UUID leaveTypeId,
			@Valid @RequestBody LeaveTypeRequest body,
			HttpServletRequest request) {
		return ok(staffLeaveService.updateLeaveType(leaveTypeId, body), "Leave type updated successfully", request);
	}

	@GetMapping("/leaves")
	@PreAuthorize("hasAuthority('LEAVE_READ')")
	public ResponseEntity<ApiResponse<PageResponse<StaffLeaveResponse>>> leaves(
			@RequestParam(required = false) UUID staffId,
			@RequestParam(required = false) LeaveStatus status,
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				staffLeaveService.leaves(staffId, status, fromDate, toDate, pageRequest),
				"Leave requests fetched successfully",
				request);
	}

	@PostMapping("/leaves")
	@PreAuthorize("hasAuthority('LEAVE_CREATE')")
	public ResponseEntity<ApiResponse<StaffLeaveResponse>> requestLeave(
			@Valid @RequestBody StaffLeaveCreateRequest body,
			HttpServletRequest request) {
		return created(staffLeaveService.requestLeave(body), "Leave requested successfully", request);
	}

	@PatchMapping("/leaves/{leaveRequestId}/approve")
	@PreAuthorize("hasAuthority('LEAVE_APPROVE')")
	public ResponseEntity<ApiResponse<StaffLeaveResponse>> approveLeave(
			@PathVariable UUID leaveRequestId,
			@RequestBody(required = false) StaffLeaveReviewRequest body,
			HttpServletRequest request) {
		return ok(staffLeaveService.approve(leaveRequestId, body), "Leave approved successfully", request);
	}

	@PatchMapping("/leaves/{leaveRequestId}/reject")
	@PreAuthorize("hasAuthority('LEAVE_APPROVE')")
	public ResponseEntity<ApiResponse<StaffLeaveResponse>> rejectLeave(
			@PathVariable UUID leaveRequestId,
			@RequestBody(required = false) StaffLeaveReviewRequest body,
			HttpServletRequest request) {
		return ok(staffLeaveService.reject(leaveRequestId, body), "Leave rejected successfully", request);
	}

	@PatchMapping("/leaves/{leaveRequestId}/cancel")
	@PreAuthorize("hasAuthority('LEAVE_CREATE')")
	public ResponseEntity<ApiResponse<StaffLeaveResponse>> cancelLeave(
			@PathVariable UUID leaveRequestId,
			@RequestBody(required = false) StaffLeaveReviewRequest body,
			HttpServletRequest request) {
		return ok(staffLeaveService.cancel(leaveRequestId, body), "Leave cancelled successfully", request);
	}

	@GetMapping("/salary-structures")
	@PreAuthorize("hasAuthority('PAYROLL_READ')")
	public ResponseEntity<ApiResponse<List<SalaryStructureResponse>>> salaryStructures(HttpServletRequest request) {
		return ok(payrollService.salaryStructures(), "Salary structures fetched successfully", request);
	}

	@PostMapping("/salary-structures")
	@PreAuthorize("hasAuthority('PAYROLL_CREATE')")
	public ResponseEntity<ApiResponse<SalaryStructureResponse>> createSalaryStructure(
			@Valid @RequestBody SalaryStructureRequest body,
			HttpServletRequest request) {
		return created(payrollService.createSalaryStructure(body), "Salary structure created successfully", request);
	}

	@PutMapping("/salary-structures/{structureId}")
	@PreAuthorize("hasAuthority('PAYROLL_UPDATE')")
	public ResponseEntity<ApiResponse<SalaryStructureResponse>> updateSalaryStructure(
			@PathVariable UUID structureId,
			@Valid @RequestBody SalaryStructureRequest body,
			HttpServletRequest request) {
		return ok(payrollService.updateSalaryStructure(structureId, body), "Salary structure updated successfully", request);
	}

	@GetMapping("/{staffId}/salary-assignments")
	@PreAuthorize("hasAuthority('PAYROLL_READ')")
	public ResponseEntity<ApiResponse<List<StaffSalaryAssignmentResponse>>> salaryAssignments(
			@PathVariable UUID staffId,
			HttpServletRequest request) {
		return ok(payrollService.salaryAssignments(staffId), "Salary assignments fetched successfully", request);
	}

	@PostMapping("/salary-assignments")
	@PreAuthorize("hasAuthority('PAYROLL_CREATE')")
	public ResponseEntity<ApiResponse<StaffSalaryAssignmentResponse>> assignSalary(
			@Valid @RequestBody StaffSalaryAssignmentRequest body,
			HttpServletRequest request) {
		return created(payrollService.assignSalary(body), "Salary assigned successfully", request);
	}

	@GetMapping("/payroll")
	@PreAuthorize("hasAuthority('PAYROLL_READ')")
	public ResponseEntity<ApiResponse<PageResponse<PayrollRecordResponse>>> payrollRecords(
			@RequestParam(required = false) UUID staffId,
			@RequestParam(required = false) Integer payrollYear,
			@RequestParam(required = false) Integer payrollMonth,
			@RequestParam(required = false) PayrollStatus status,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				payrollService.payrollRecords(staffId, payrollYear, payrollMonth, status, pageRequest),
				"Payroll records fetched successfully",
				request);
	}

	@PostMapping("/payroll/generate")
	@PreAuthorize("hasAuthority('PAYROLL_PROCESS')")
	public ResponseEntity<ApiResponse<PayrollRecordResponse>> generatePayroll(
			@Valid @RequestBody PayrollGenerateRequest body,
			HttpServletRequest request) {
		return created(payrollService.generatePayroll(body), "Payroll generated successfully", request);
	}

	@PatchMapping("/payroll/{payrollRecordId}/paid")
	@PreAuthorize("hasAuthority('PAYROLL_PROCESS')")
	public ResponseEntity<ApiResponse<PayrollRecordResponse>> markPayrollPaid(
			@PathVariable UUID payrollRecordId,
			HttpServletRequest request) {
		return ok(payrollService.markPaid(payrollRecordId), "Payroll marked paid successfully", request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(response(data, message, request));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(response(data, message, request));
	}

	private <T> ApiResponse<T> response(T data, String message, HttpServletRequest request) {
		return ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID));
	}
}
