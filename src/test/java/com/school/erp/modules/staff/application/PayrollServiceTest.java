package com.school.erp.modules.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.staff.api.dto.PayrollGenerateRequest;
import com.school.erp.modules.staff.api.dto.SalaryStructureRequest;
import com.school.erp.modules.staff.api.dto.StaffSalaryAssignmentRequest;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.PayrollRecord;
import com.school.erp.modules.staff.domain.PayrollStatus;
import com.school.erp.modules.staff.domain.SalaryStructure;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffSalaryAssignment;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.PayrollRecordRepository;
import com.school.erp.modules.staff.infrastructure.SalaryStructureRepository;
import com.school.erp.modules.staff.infrastructure.StaffSalaryAssignmentRepository;
import com.school.erp.modules.students.domain.Gender;

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
class PayrollServiceTest {

	@Mock
	private SalaryStructureRepository salaryStructureRepository;

	@Mock
	private StaffSalaryAssignmentRepository salaryAssignmentRepository;

	@Mock
	private PayrollRecordRepository payrollRecordRepository;

	@Mock
	private StaffService staffService;

	@Mock
	private AuditLogService auditLogService;

	private PayrollService payrollService;
	private Staff staff;
	private SalaryStructure salaryStructure;

	@BeforeEach
	void setUp() {
		payrollService = new PayrollService(
				salaryStructureRepository,
				salaryAssignmentRepository,
				payrollRecordRepository,
				staffService,
				new StaffMapper(),
				auditLogService);
		Department department = new Department("Administration", null, true);
		Designation designation = new Designation("Accountant", department, null, true);
		setId(department);
		setId(designation);
		staff = new Staff(
				"EMP-030",
				"Kavita",
				null,
				"Rao",
				Gender.FEMALE,
				LocalDate.of(1985, 2, 2),
				"kavita.rao@school.test",
				"9890000040",
				null,
				null,
				department,
				designation,
				LocalDate.of(2020, 6, 1),
				StaffType.NON_TEACHING,
				EmploymentStatus.ACTIVE);
		setId(staff);
		salaryStructure = new SalaryStructure(
				"ADM-01",
				"Admin Salary",
				new BigDecimal("30000.00"),
				new BigDecimal("5000.00"),
				new BigDecimal("2000.00"),
				true);
		setId(salaryStructure);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void salaryStructureCrudCalculatesPublicTotals() {
		authenticate("PAYROLL_CREATE", "PAYROLL_UPDATE");
		when(salaryStructureRepository.findByCodeIgnoreCaseAndDeletedFalse("TCH-01")).thenReturn(java.util.Optional.empty());
		when(salaryStructureRepository.save(any(SalaryStructure.class))).thenAnswer(invocation -> {
			SalaryStructure persisted = invocation.getArgument(0);
			setId(persisted);
			return persisted;
		});
		when(salaryStructureRepository.findByIdAndDeletedFalse(salaryStructure.getId()))
				.thenReturn(Optional.of(salaryStructure));
		when(salaryStructureRepository.findByCodeIgnoreCaseAndDeletedFalse("ADM-02")).thenReturn(Optional.empty());

		var created = payrollService.createSalaryStructure(new SalaryStructureRequest(
				"TCH-01",
				"Teacher Salary",
				new BigDecimal("40000.00"),
				new BigDecimal("8000.00"),
				new BigDecimal("3000.00"),
				null));
		var updated = payrollService.updateSalaryStructure(salaryStructure.getId(), new SalaryStructureRequest(
				"ADM-02",
				"Senior Admin Salary",
				new BigDecimal("36000.00"),
				new BigDecimal("6000.00"),
				new BigDecimal("2500.00"),
				false));

		assertThat(created.grossSalary()).isEqualByComparingTo("48000.00");
		assertThat(created.netSalary()).isEqualByComparingTo("45000.00");
		assertThat(created.active()).isTrue();
		assertThat(updated.code()).isEqualTo("ADM-02");
		assertThat(updated.netSalary()).isEqualByComparingTo("39500.00");
		assertThat(updated.active()).isFalse();
	}

	@Test
	void assignSalaryClosesPriorActiveAssignmentAndCreatesNewOne() {
		authenticate("PAYROLL_CREATE");
		SalaryStructure revisedStructure = salaryStructure(
				"ADM-02",
				"Revised Admin Salary",
				new BigDecimal("36000.00"),
				new BigDecimal("6000.00"),
				new BigDecimal("2500.00"));
		StaffSalaryAssignment oldAssignment = new StaffSalaryAssignment(
				staff,
				salaryStructure,
				LocalDate.of(2026, 1, 1));
		setId(oldAssignment);
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(salaryStructureRepository.findByIdAndDeletedFalse(revisedStructure.getId()))
				.thenReturn(Optional.of(revisedStructure));
		when(salaryAssignmentRepository.findByStaffIdAndDeletedFalseOrderByEffectiveFromDesc(staff.getId()))
				.thenReturn(List.of(oldAssignment));
		when(salaryAssignmentRepository.save(any(StaffSalaryAssignment.class))).thenAnswer(invocation -> {
			StaffSalaryAssignment persisted = invocation.getArgument(0);
			setId(persisted);
			return persisted;
		});

		var response = payrollService.assignSalary(new StaffSalaryAssignmentRequest(
				staff.getId(),
				revisedStructure.getId(),
				LocalDate.of(2026, 7, 1)));

		assertThat(response.salaryStructureName()).isEqualTo("Revised Admin Salary");
		assertThat(response.effectiveFrom()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(oldAssignment.isActive()).isFalse();
		assertThat(oldAssignment.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 6, 30));
	}

	@Test
	void generatePayrollUsesEffectiveSalaryAssignmentAndCalculatesTotals() {
		StaffSalaryAssignment assignment = new StaffSalaryAssignment(
				staff,
				salaryStructure,
				LocalDate.of(2026, 1, 1));
		setId(assignment);
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(payrollRecordRepository.existsByStaffIdAndPayrollYearAndPayrollMonthAndDeletedFalse(staff.getId(), 2026, 8))
				.thenReturn(false);
		when(salaryAssignmentRepository.findEffectiveAssignments(staff.getId(), LocalDate.of(2026, 8, 31)))
				.thenReturn(List.of(assignment));
		when(payrollRecordRepository.save(org.mockito.ArgumentMatchers.any()))
				.thenAnswer(invocation -> {
					Object record = invocation.getArgument(0);
					setId(record);
					return record;
				});

		var response = payrollService.generatePayroll(new PayrollGenerateRequest(staff.getId(), 2026, 8));

		assertThat(response.salaryStructureName()).isEqualTo("Admin Salary");
		assertThat(response.grossSalary()).isEqualByComparingTo("35000.00");
		assertThat(response.netSalary()).isEqualByComparingTo("33000.00");
	}

	@Test
	void generatePayrollRejectsDuplicateStaffPeriod() {
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(payrollRecordRepository.existsByStaffIdAndPayrollYearAndPayrollMonthAndDeletedFalse(staff.getId(), 2026, 8))
				.thenReturn(true);

		assertThatThrownBy(() -> payrollService.generatePayroll(new PayrollGenerateRequest(staff.getId(), 2026, 8)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void generatedPayrollRecordPreservesSalarySnapshotAfterStructureChanges() {
		PayrollRecord record = new PayrollRecord(staff, 2026, 8, salaryStructure);
		setId(record);
		salaryStructure.update(
				"ADM-99",
				"Future Admin Salary",
				new BigDecimal("1.00"),
				new BigDecimal("2.00"),
				new BigDecimal("3.00"),
				true);

		assertThat(record.getSalaryStructureName()).isEqualTo("Admin Salary");
		assertThat(record.getBasicSalary()).isEqualByComparingTo("30000.00");
		assertThat(record.getGrossSalary()).isEqualByComparingTo("35000.00");
		assertThat(record.getNetSalary()).isEqualByComparingTo("33000.00");
	}

	@Test
	void markPaidSetsPaidStatusAndKeepsSnapshotValues() {
		authenticate("PAYROLL_PROCESS");
		PayrollRecord record = new PayrollRecord(staff, 2026, 8, salaryStructure);
		setId(record);
		when(payrollRecordRepository.findByIdAndDeletedFalse(record.getId())).thenReturn(Optional.of(record));

		var response = payrollService.markPaid(record.getId());

		assertThat(response.status()).isEqualTo(PayrollStatus.PAID);
		assertThat(response.paidAt()).isNotNull();
		assertThat(response.salaryStructureName()).isEqualTo("Admin Salary");
		assertThat(response.netSalary()).isEqualByComparingTo("33000.00");
	}

	@Test
	void payrollMutationsRejectAuthenticatedUsersWithoutPayrollAuthority() {
		authenticate("STAFF_READ");

		assertThatThrownBy(() -> payrollService.generatePayroll(new PayrollGenerateRequest(staff.getId(), 2026, 8)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	private SalaryStructure salaryStructure(
			String code,
			String name,
			BigDecimal basicSalary,
			BigDecimal allowances,
			BigDecimal deductions) {
		SalaryStructure structure = new SalaryStructure(code, name, basicSalary, allowances, deductions, true);
		setId(structure);
		return structure;
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"payroll-user",
				null,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
