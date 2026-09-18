package com.school.erp.modules.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.staff.api.dto.DepartmentRequest;
import com.school.erp.modules.staff.api.dto.DesignationRequest;
import com.school.erp.modules.staff.api.dto.StaffDocumentRequest;
import com.school.erp.modules.staff.api.dto.StaffExitRequest;
import com.school.erp.modules.staff.api.dto.StaffRequest;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffDocument;
import com.school.erp.modules.staff.domain.StaffDocumentStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.DepartmentRepository;
import com.school.erp.modules.staff.infrastructure.DesignationRepository;
import com.school.erp.modules.staff.infrastructure.StaffDocumentRepository;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

	@Mock
	private DepartmentRepository departmentRepository;

	@Mock
	private DesignationRepository designationRepository;

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private StaffDocumentRepository documentRepository;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private TeacherRepository teacherRepository;

	@Mock
	private AuditLogService auditLogService;

	private StaffService staffService;
	private Department department;
	private Designation designation;

	@BeforeEach
	void setUp() {
		staffService = new StaffService(
				departmentRepository,
				designationRepository,
				staffRepository,
				documentRepository,
				userAccountRepository,
				teacherRepository,
				new StaffMapper(),
				auditLogService);
		department = new Department("Teaching", "Academic staff", true);
		designation = new Designation("Teacher", department, "Class teacher", true);
		setId(department);
		setId(designation);
	}

	@Test
	void createDepartmentAndDesignationPersistCanonicalMasters() {
		when(departmentRepository.findByNameIgnoreCaseAndDeletedFalse("Operations")).thenReturn(Optional.empty());
		when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));
		when(designationRepository.findByNameIgnoreCaseAndDeletedFalse("Coordinator")).thenReturn(Optional.empty());
		when(departmentRepository.findByIdAndDeletedFalse(department.getId())).thenReturn(Optional.of(department));
		when(designationRepository.save(any(Designation.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var departmentResponse = staffService.createDepartment(new DepartmentRequest(
				"Operations",
				"Administrative operations",
				null));
		var designationResponse = staffService.createDesignation(new DesignationRequest(
				"Coordinator",
				department.getId(),
				"Operations coordinator",
				true));

		assertThat(departmentResponse.name()).isEqualTo("Operations");
		assertThat(departmentResponse.active()).isTrue();
		assertThat(designationResponse.name()).isEqualTo("Coordinator");
		assertThat(designationResponse.departmentId()).isEqualTo(department.getId());
	}

	@Test
	void updateDepartmentAndDesignationApplyChangesToCanonicalMasters() {
		when(departmentRepository.findByIdAndDeletedFalse(department.getId())).thenReturn(Optional.of(department));
		when(departmentRepository.findByNameIgnoreCaseAndDeletedFalse("Student Affairs")).thenReturn(Optional.empty());
		when(designationRepository.findByIdAndDeletedFalse(designation.getId())).thenReturn(Optional.of(designation));
		when(designationRepository.findByNameIgnoreCaseAndDeletedFalse("Senior Teacher")).thenReturn(Optional.empty());

		var departmentResponse = staffService.updateDepartment(department.getId(), new DepartmentRequest(
				"Student Affairs",
				"Student support and records",
				true));
		var designationResponse = staffService.updateDesignation(designation.getId(), new DesignationRequest(
				"Senior Teacher",
				department.getId(),
				"Senior teaching role",
				true));

		assertThat(departmentResponse.name()).isEqualTo("Student Affairs");
		assertThat(departmentResponse.description()).isEqualTo("Student support and records");
		assertThat(designationResponse.name()).isEqualTo("Senior Teacher");
		assertThat(designationResponse.departmentName()).isEqualTo("Student Affairs");
	}

	@Test
	void staffSearchAppliesFiltersAndPageRequest() {
		Staff listedStaff = staff("EMP-050");
		when(staffRepository.search(
				eq(EmploymentStatus.ACTIVE),
				eq(StaffType.NON_TEACHING),
				eq(department.getId()),
				eq(designation.getId()),
				any()))
				.thenReturn(new PageImpl<>(List.of(listedStaff)));

		var response = staffService.staff(
				EmploymentStatus.ACTIVE,
				StaffType.NON_TEACHING,
				department.getId(),
				designation.getId(),
				new PageRequestDto(0, 20, null, null));

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().getFirst().employeeCode()).isEqualTo("EMP-050");
	}

	@Test
	void createStaffLinksTeacherAndForcesTeachingStaffType() {
		UUID teacherId = UUID.randomUUID();
		Teacher teacher = teacher(teacherId);
		when(staffRepository.findByEmployeeCodeIgnoreCaseAndDeletedFalse("EMP-001")).thenReturn(Optional.empty());
		when(departmentRepository.findByIdAndDeletedFalse(department.getId())).thenReturn(Optional.of(department));
		when(designationRepository.findByIdAndDeletedFalse(designation.getId())).thenReturn(Optional.of(designation));
		when(teacherRepository.findByIdAndDeletedFalse(teacherId)).thenReturn(Optional.of(teacher));
		when(staffRepository.findByTeacherIdAndDeletedFalse(teacherId)).thenReturn(Optional.empty());
		when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var response = staffService.createStaff(staffRequest("EMP-001", teacherId, StaffType.NON_TEACHING));

		assertThat(response.teacherId()).isEqualTo(teacherId);
		assertThat(response.staffType()).isEqualTo(StaffType.TEACHING);
		assertThat(teacher.getStaffId()).isEqualTo(response.id());
		verify(teacherRepository).save(teacher);
	}

	@Test
	void createStaffLinksExistingUserAccountWhenProvided() {
		UUID userAccountId = UUID.randomUUID();
		when(staffRepository.findByEmployeeCodeIgnoreCaseAndDeletedFalse("EMP-003")).thenReturn(Optional.empty());
		when(departmentRepository.findByIdAndDeletedFalse(department.getId())).thenReturn(Optional.of(department));
		when(designationRepository.findByIdAndDeletedFalse(designation.getId())).thenReturn(Optional.of(designation));
		when(userAccountRepository.findByIdAndDeletedFalse(userAccountId)).thenReturn(Optional.of(mock(UserAccount.class)));
		when(staffRepository.save(any(Staff.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var response = staffService.createStaff(staffRequest("EMP-003", userAccountId, null, StaffType.NON_TEACHING));

		assertThat(response.userAccountId()).isEqualTo(userAccountId);
		verify(userAccountRepository).findByIdAndDeletedFalse(userAccountId);
	}

	@Test
	void updateStaffRemovingTeacherClearsOldTeacherBackReference() {
		UUID staffId = UUID.randomUUID();
		UUID oldTeacherId = UUID.randomUUID();
		Teacher oldTeacher = teacher(oldTeacherId);
		Staff staff = new Staff(
				"EMP-002",
				"Asha",
				null,
				"Rao",
				Gender.FEMALE,
				LocalDate.of(1990, 5, 10),
				"asha.rao@school.test",
				"9890000001",
				null,
				oldTeacherId,
				department,
				designation,
				LocalDate.of(2022, 6, 1),
				StaffType.TEACHING,
				EmploymentStatus.ACTIVE);
		setId(staff, staffId);
		oldTeacher.linkStaff(staffId);
		when(staffRepository.findByIdAndDeletedFalse(staffId)).thenReturn(Optional.of(staff));
		when(staffRepository.findByEmployeeCodeIgnoreCaseAndDeletedFalse("EMP-002")).thenReturn(Optional.empty());
		when(departmentRepository.findByIdAndDeletedFalse(department.getId())).thenReturn(Optional.of(department));
		when(designationRepository.findByIdAndDeletedFalse(designation.getId())).thenReturn(Optional.of(designation));
		when(teacherRepository.findByIdAndDeletedFalse(oldTeacherId)).thenReturn(Optional.of(oldTeacher));

		var response = staffService.updateStaff(staffId, staffRequest("EMP-002", null, StaffType.NON_TEACHING));

		assertThat(response.teacherId()).isNull();
		assertThat(response.staffType()).isEqualTo(StaffType.NON_TEACHING);
		assertThat(oldTeacher.getStaffId()).isNull();
		verify(teacherRepository).save(oldTeacher);
	}

	@Test
	void getAndDeactivateStaffUseCanonicalLifecycleState() {
		UUID staffId = UUID.randomUUID();
		Staff activeStaff = staff("EMP-055");
		setId(activeStaff, staffId);
		when(staffRepository.findByIdAndDeletedFalse(staffId)).thenReturn(Optional.of(activeStaff));

		var fetched = staffService.getStaff(staffId);
		var deactivated = staffService.deactivateStaff(staffId);

		assertThat(fetched.employeeCode()).isEqualTo("EMP-055");
		assertThat(deactivated.status()).isEqualTo(EmploymentStatus.INACTIVE);
	}

	@Test
	void exitStaffRecordsRelievingDateAndReason() {
		UUID staffId = UUID.randomUUID();
		Staff activeStaff = staff("EMP-056");
		setId(activeStaff, staffId);
		when(staffRepository.findByIdAndDeletedFalse(staffId)).thenReturn(Optional.of(activeStaff));

		var response = staffService.exitStaff(staffId, new StaffExitRequest(
				LocalDate.of(2026, 8, 31),
				"Relocated"));

		assertThat(response.status()).isEqualTo(EmploymentStatus.EXITED);
		assertThat(response.relievingDate()).isEqualTo(LocalDate.of(2026, 8, 31));
		assertThat(response.exitReason()).isEqualTo("Relocated");
	}

	@Test
	void exitStaffRejectsRelievingDateBeforeJoiningDate() {
		UUID staffId = UUID.randomUUID();
		Staff activeStaff = staff("EMP-060");
		setId(activeStaff, staffId);
		when(staffRepository.findByIdAndDeletedFalse(staffId)).thenReturn(Optional.of(activeStaff));

		assertThatThrownBy(() -> staffService.exitStaff(staffId, new StaffExitRequest(
				LocalDate.of(2024, 5, 31),
				"Backdated exit")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void staffDocumentsCanBeCreatedUpdatedAndArchived() {
		UUID staffId = UUID.randomUUID();
		Staff activeStaff = staff("EMP-070");
		setId(activeStaff, staffId);
		when(staffRepository.findByIdAndDeletedFalse(staffId)).thenReturn(Optional.of(activeStaff));
		when(documentRepository.save(any(StaffDocument.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

		var created = staffService.createDocument(staffId, new StaffDocumentRequest(
				"Identity",
				"identity.pdf",
				null,
				"staff/identity.pdf",
				null));
		StaffDocument document = new StaffDocument(
				activeStaff,
				"Identity",
				"identity.pdf",
				null,
				"staff/identity.pdf",
				"principal",
				StaffDocumentStatus.ACTIVE);
		setId(document, created.id());
		when(documentRepository.findByIdAndDeletedFalse(created.id())).thenReturn(Optional.of(document));

		var updated = staffService.updateDocument(staffId, created.id(), new StaffDocumentRequest(
				"Contract",
				"contract.pdf",
				"https://files.example.test/contract.pdf",
				null,
				StaffDocumentStatus.ARCHIVED));
		staffService.deleteDocument(staffId, created.id());

		assertThat(created.status()).isEqualTo(StaffDocumentStatus.ACTIVE);
		assertThat(updated.documentType()).isEqualTo("Contract");
		assertThat(updated.status()).isEqualTo(StaffDocumentStatus.ARCHIVED);
		assertThat(document.isDeleted()).isTrue();
	}

	private StaffRequest staffRequest(String employeeCode, UUID teacherId, StaffType staffType) {
		return staffRequest(employeeCode, null, teacherId, staffType);
	}

	private StaffRequest staffRequest(String employeeCode, UUID userAccountId, UUID teacherId, StaffType staffType) {
		return new StaffRequest(
				employeeCode,
				"Asha",
				null,
				"Rao",
				Gender.FEMALE,
				LocalDate.of(1990, 5, 10),
				"asha.rao@school.test",
				"9890000001",
				userAccountId,
				teacherId,
				department.getId(),
				designation.getId(),
				LocalDate.of(2022, 6, 1),
				staffType,
				EmploymentStatus.ACTIVE);
	}

	private Teacher teacher(UUID id) {
		Teacher teacher = new Teacher("T-001", "Asha", "Rao", "asha.rao@school.test", "9890000001");
		setId(teacher, id);
		return teacher;
	}

	private Staff staff(String employeeCode) {
		Staff staff = new Staff(
				employeeCode,
				"Asha",
				null,
				"Rao",
				Gender.FEMALE,
				LocalDate.of(1990, 5, 10),
				"asha.rao@school.test",
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

	private <T> T persisted(T entity) {
		if (ReflectionTestUtils.getField(entity, "id") == null) {
			setId(entity);
		}
		return entity;
	}

	private void setId(Object entity) {
		setId(entity, UUID.randomUUID());
	}

	private void setId(Object entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
	}
}
