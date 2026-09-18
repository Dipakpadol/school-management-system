package com.school.erp.modules.staff.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.staff.api.dto.PayrollGenerateRequest;
import com.school.erp.modules.staff.api.dto.PayrollRecordResponse;
import com.school.erp.modules.staff.api.dto.SalaryStructureRequest;
import com.school.erp.modules.staff.api.dto.SalaryStructureResponse;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentRequest;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentResponse;
import com.school.erp.modules.staff.domain.PayrollRecord;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.SalaryStructure;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffSalaryAssignment;
import com.school.erp.modules.staff.infrastructure.PayrollRecordRepository;
import com.school.erp.modules.staff.infrastructure.SalaryStructureRepository;
import com.school.erp.modules.staff.infrastructure.StaffSalaryAssignmentRepository;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayrollService {

	private static final String MODULE_NAME = "PAYROLL";

	private final SalaryStructureRepository salaryStructureRepository;
	private final StaffSalaryAssignmentRepository salaryAssignmentRepository;
	private final PayrollRecordRepository payrollRecordRepository;
	private final StaffService staffService;
	private final StaffMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<SalaryStructureResponse> salaryStructures() {
		requireAnyAuthority("PAYROLL_READ", "PAYROLL_CREATE", "PAYROLL_UPDATE", "PAYROLL_PROCESS");
		return salaryStructureRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toSalaryStructureResponse)
				.toList();
	}

	@Transactional
	public SalaryStructureResponse createSalaryStructure(SalaryStructureRequest request) {
		requireAnyAuthority("PAYROLL_CREATE", "PAYROLL_UPDATE", "PAYROLL_PROCESS");
		validateSalaryCode(request.code(), null);
		SalaryStructure structure = salaryStructureRepository.save(new SalaryStructure(
				request.code(),
				request.name(),
				request.basicSalary(),
				request.allowances(),
				request.deductions(),
				active(request.active())));
		SalaryStructureResponse response = mapper.toSalaryStructureResponse(structure);
		audit("SalaryStructure", structure.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public SalaryStructureResponse updateSalaryStructure(UUID structureId, SalaryStructureRequest request) {
		requireAnyAuthority("PAYROLL_UPDATE", "PAYROLL_PROCESS");
		SalaryStructure structure = loadSalaryStructure(structureId);
		SalaryStructureResponse oldValue = mapper.toSalaryStructureResponse(structure);
		validateSalaryCode(request.code(), structureId);
		structure.update(
				request.code(),
				request.name(),
				request.basicSalary(),
				request.allowances(),
				request.deductions(),
				active(request.active()));
		SalaryStructureResponse response = mapper.toSalaryStructureResponse(structure);
		audit("SalaryStructure", structureId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public List<StaffSalaryAssignmentResponse> salaryAssignments(UUID staffId) {
		requireAnyAuthority("PAYROLL_READ", "PAYROLL_CREATE", "PAYROLL_UPDATE", "PAYROLL_PROCESS");
		staffService.loadStaff(staffId);
		return salaryAssignmentRepository.findByStaffIdAndDeletedFalseOrderByEffectiveFromDesc(staffId).stream()
				.map(mapper::toSalaryAssignmentResponse)
				.toList();
	}

	@Transactional
	public StaffSalaryAssignmentResponse assignSalary(StaffSalaryAssignmentRequest request) {
		requireAnyAuthority("PAYROLL_CREATE", "PAYROLL_UPDATE", "PAYROLL_PROCESS");
		Staff staff = staffService.loadActiveStaff(request.staffId());
		SalaryStructure structure = loadActiveSalaryStructure(request.salaryStructureId());
		salaryAssignmentRepository.findByStaffIdAndDeletedFalseOrderByEffectiveFromDesc(staff.getId()).stream()
				.filter(StaffSalaryAssignment::isActive)
				.filter(existing -> !existing.getEffectiveFrom().isAfter(request.effectiveFrom()))
				.forEach(existing -> existing.close(request.effectiveFrom().minusDays(1)));
		StaffSalaryAssignment assignment = salaryAssignmentRepository.save(new StaffSalaryAssignment(
				staff,
				structure,
				request.effectiveFrom()));
		StaffSalaryAssignmentResponse response = mapper.toSalaryAssignmentResponse(assignment);
		audit("StaffSalaryAssignment", assignment.getId(), "ASSIGN", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<PayrollRecordResponse> payrollRecords(
			UUID staffId,
			Integer payrollYear,
			Integer payrollMonth,
			PayrollStatus status,
			PageRequestDto pageRequest) {
		requireAnyAuthority("PAYROLL_READ", "PAYROLL_PROCESS");
		return PageResponse.from(
				payrollRecordRepository.search(staffId, payrollYear, payrollMonth, status, pageRequest.toPageable("payrollYear")),
				mapper::toPayrollRecordResponse);
	}

	@Transactional
	public PayrollRecordResponse generatePayroll(PayrollGenerateRequest request) {
		requireAnyAuthority("PAYROLL_PROCESS", "PAYROLL_CREATE");
		validatePeriod(request.payrollYear(), request.payrollMonth());
		Staff staff = staffService.loadActiveStaff(request.staffId());
		if (payrollRecordRepository.existsByStaffIdAndPayrollYearAndPayrollMonthAndDeletedFalse(
				staff.getId(),
				request.payrollYear(),
				request.payrollMonth())) {
			throw new BusinessException(ErrorCode.CONFLICT, "Payroll already exists for staff and period.");
		}
		LocalDate periodEnd = YearMonth.of(request.payrollYear(), request.payrollMonth()).atEndOfMonth();
		StaffSalaryAssignment assignment = salaryAssignmentRepository
				.findEffectiveAssignments(staff.getId(), periodEnd)
				.stream()
				.findFirst()
				.orElseThrow(() -> new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"No salary structure is assigned for the payroll period."));
		PayrollRecord record = payrollRecordRepository.save(new PayrollRecord(
				staff,
				request.payrollYear(),
				request.payrollMonth(),
				assignment.getSalaryStructure()));
		PayrollRecordResponse response = mapper.toPayrollRecordResponse(record);
		audit("PayrollRecord", record.getId(), "GENERATE", null, response);
		return response;
	}

	@Transactional
	public PayrollRecordResponse markPaid(UUID payrollRecordId) {
		requireAnyAuthority("PAYROLL_PROCESS", "PAYROLL_UPDATE");
		PayrollRecord record = loadPayrollRecord(payrollRecordId);
		PayrollRecordResponse oldValue = mapper.toPayrollRecordResponse(record);
		if (record.getStatus() == PayrollStatus.PAID) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payroll record is already paid.");
		}
		record.markPaid();
		PayrollRecordResponse response = mapper.toPayrollRecordResponse(record);
		audit("PayrollRecord", payrollRecordId, "PAID", oldValue, response);
		return response;
	}

	public List<Map<String, Object>> reportRows(
			UUID staffId,
			Integer payrollYear,
			Integer payrollMonth,
			PayrollStatus status) {
		requireAnyAuthority("PAYROLL_READ", "PAYROLL_PROCESS");
		return payrollRecordRepository.findReportRows(staffId, payrollYear, payrollMonth, status).stream()
				.map(this::payrollRow)
				.toList();
	}

	private Map<String, Object> payrollRow(PayrollRecord record) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Employee Code", record.getStaff().getEmployeeCode());
		row.put("Staff Name", record.getStaff().getDisplayName());
		row.put("Period", "%04d-%02d".formatted(record.getPayrollYear(), record.getPayrollMonth()));
		row.put("Salary Structure", record.getSalaryStructureName());
		row.put("Basic", record.getBasicSalary());
		row.put("Allowances", record.getAllowances());
		row.put("Deductions", record.getDeductions());
		row.put("Gross", record.getGrossSalary());
		row.put("Net", record.getNetSalary());
		row.put("Status", record.getStatus());
		return row;
	}

	private SalaryStructure loadSalaryStructure(UUID structureId) {
		return salaryStructureRepository.findByIdAndDeletedFalse(structureId)
				.orElseThrow(() -> new ResourceNotFoundException("Salary structure", structureId));
	}

	private SalaryStructure loadActiveSalaryStructure(UUID structureId) {
		SalaryStructure structure = loadSalaryStructure(structureId);
		if (!structure.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Salary structure is inactive.");
		}
		return structure;
	}

	private PayrollRecord loadPayrollRecord(UUID payrollRecordId) {
		return payrollRecordRepository.findByIdAndDeletedFalse(payrollRecordId)
				.orElseThrow(() -> new ResourceNotFoundException("Payroll record", payrollRecordId));
	}

	private void validateSalaryCode(String code, UUID excludedId) {
		salaryStructureRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Salary structure code already exists.");
				});
	}

	private void validatePeriod(int payrollYear, int payrollMonth) {
		if (payrollYear < 2000 || payrollYear > 2100 || payrollMonth < 1 || payrollMonth > 12) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Payroll period is invalid.");
		}
	}

	private boolean active(Boolean active) {
		return active == null || active;
	}

	private void requireAnyAuthority(String... authorities) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		for (String authority : authorities) {
			boolean allowed = authentication.getAuthorities().stream()
					.anyMatch(granted -> granted.getAuthority().equals(authority));
			if (allowed) {
				return;
			}
		}
		throw new BusinessException(ErrorCode.FORBIDDEN, "Payroll permission is required.");
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}
}
