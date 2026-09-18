package com.school.erp.modules.staff.application;

import com.school.erp.modules.staff.api.dto.DepartmentResponse;
import com.school.erp.modules.staff.api.dto.DesignationResponse;
import com.school.erp.modules.staff.api.dto.LeaveTypeResponse;
import com.school.erp.modules.staff.api.dto.PayrollRecordResponse;
import com.school.erp.modules.staff.api.dto.SalaryStructureResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceRecordResponse;
import com.school.erp.modules.staff.api.dto.StaffDocumentResponse;
import com.school.erp.modules.staff.api.dto.StaffLeaveResponse;
import com.school.erp.modules.staff.api.dto.StaffResponse;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentResponse;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.LeaveType;
import com.school.erp.modules.staff.domain.PayrollRecord;
import com.school.erp.modules.staff.domain.SalaryStructure;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffAttendance;
import com.school.erp.modules.staff.domain.StaffDocument;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;
import com.school.erp.modules.staff.domain.StaffSalaryAssignment;

import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

	public DepartmentResponse toDepartmentResponse(Department department) {
		return new DepartmentResponse(
				department.getId(),
				department.getName(),
				department.getDescription(),
				department.isActive());
	}

	public DesignationResponse toDesignationResponse(Designation designation) {
		Department department = designation.getDepartment();
		return new DesignationResponse(
				designation.getId(),
				designation.getName(),
				department == null ? null : department.getId(),
				department == null ? null : department.getName(),
				designation.getDescription(),
				designation.isActive());
	}

	public StaffResponse toStaffResponse(Staff staff) {
		Department department = staff.getDepartment();
		Designation designation = staff.getDesignation();
		return new StaffResponse(
				staff.getId(),
				staff.getEmployeeCode(),
				staff.getFirstName(),
				staff.getMiddleName(),
				staff.getLastName(),
				staff.getDisplayName(),
				staff.getGender(),
				staff.getDateOfBirth(),
				staff.getEmail(),
				staff.getPhoneNumber(),
				staff.getUserAccountId(),
				staff.getTeacherId(),
				department == null ? null : department.getId(),
				department == null ? null : department.getName(),
				designation == null ? null : designation.getId(),
				designation == null ? null : designation.getName(),
				staff.getJoiningDate(),
				staff.getStaffType(),
				staff.getStatus(),
				staff.getRelievingDate(),
				staff.getExitReason());
	}

	public StaffDocumentResponse toDocumentResponse(StaffDocument document) {
		return new StaffDocumentResponse(
				document.getId(),
				document.getStaff().getId(),
				document.getDocumentType(),
				document.getFileName(),
				document.getFileUrl(),
				document.getFilePath(),
				document.getUploadedAt(),
				document.getUploadedBy(),
				document.getStatus());
	}

	public StaffAttendanceRecordResponse toAttendanceResponse(
			Staff staff,
			java.time.LocalDate date,
			com.school.erp.modules.attendance.domain.AttendanceStatus status,
			String remarks,
			boolean approvedLeave) {
		Department department = staff.getDepartment();
		Designation designation = staff.getDesignation();
		return new StaffAttendanceRecordResponse(
				staff.getId(),
				staff.getEmployeeCode(),
				staff.getDisplayName(),
				department == null ? null : department.getId(),
				department == null ? null : department.getName(),
				designation == null ? null : designation.getId(),
				designation == null ? null : designation.getName(),
				date,
				status,
				remarks,
				approvedLeave);
	}

	public StaffAttendanceRecordResponse toAttendanceResponse(StaffAttendance attendance, boolean approvedLeave) {
		return toAttendanceResponse(
				attendance.getStaff(),
				attendance.getAttendanceDate(),
				attendance.getStatus(),
				attendance.getRemarks(),
				approvedLeave);
	}

	public LeaveTypeResponse toLeaveTypeResponse(LeaveType leaveType) {
		return new LeaveTypeResponse(
				leaveType.getId(),
				leaveType.getName(),
				leaveType.getDescription(),
				leaveType.isPaid(),
				leaveType.isActive());
	}

	public StaffLeaveResponse toLeaveResponse(StaffLeaveRequest request) {
		return new StaffLeaveResponse(
				request.getId(),
				request.getStaff().getId(),
				request.getStaff().getEmployeeCode(),
				request.getStaff().getDisplayName(),
				request.getLeaveType().getId(),
				request.getLeaveType().getName(),
				request.getStartDate(),
				request.getEndDate(),
				request.getDurationDays(),
				request.getReason(),
				request.getStatus(),
				request.getRequestedBy(),
				request.getReviewedBy(),
				request.getReviewedAt(),
				request.getReviewComment());
	}

	public SalaryStructureResponse toSalaryStructureResponse(SalaryStructure salaryStructure) {
		return new SalaryStructureResponse(
				salaryStructure.getId(),
				salaryStructure.getCode(),
				salaryStructure.getName(),
				salaryStructure.getBasicSalary(),
				salaryStructure.getAllowances(),
				salaryStructure.getDeductions(),
				salaryStructure.grossSalary(),
				salaryStructure.netSalary(),
				salaryStructure.isActive());
	}

	public StaffSalaryAssignmentResponse toSalaryAssignmentResponse(StaffSalaryAssignment assignment) {
		return new StaffSalaryAssignmentResponse(
				assignment.getId(),
				assignment.getStaff().getId(),
				assignment.getStaff().getEmployeeCode(),
				assignment.getStaff().getDisplayName(),
				assignment.getSalaryStructure().getId(),
				assignment.getSalaryStructure().getName(),
				assignment.getEffectiveFrom(),
				assignment.getEffectiveTo(),
				assignment.isActive());
	}

	public PayrollRecordResponse toPayrollRecordResponse(PayrollRecord record) {
		return new PayrollRecordResponse(
				record.getId(),
				record.getStaff().getId(),
				record.getStaff().getEmployeeCode(),
				record.getStaff().getDisplayName(),
				record.getPayrollYear(),
				record.getPayrollMonth(),
				record.getSalaryStructureName(),
				record.getBasicSalary(),
				record.getAllowances(),
				record.getDeductions(),
				record.getGrossSalary(),
				record.getNetSalary(),
				record.getStatus(),
				record.getGeneratedAt(),
				record.getPaidAt());
	}
}
