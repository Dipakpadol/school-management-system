package com.school.erp.modules.students.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.application.StudentFeeAutoAssignmentService;
import com.school.erp.modules.hostel.application.HostelService;
import com.school.erp.modules.transport.application.TransportService;
import com.school.erp.modules.students.api.dto.ClassSectionAssignmentRequest;
import com.school.erp.modules.students.api.dto.ParentGuardianRequest;
import com.school.erp.modules.students.api.dto.ParentMappingRequest;
import com.school.erp.modules.students.api.dto.StudentAdmissionRequest;
import com.school.erp.modules.students.api.dto.StudentDocumentRequest;
import com.school.erp.modules.students.api.dto.StudentProfileRequest;
import com.school.erp.modules.students.api.dto.StudentResponse;
import com.school.erp.modules.students.api.dto.StudentSearchRequest;
import com.school.erp.modules.students.api.dto.StudentSummaryResponse;
import com.school.erp.modules.students.domain.DocumentType;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.ParentGuardian;
import com.school.erp.modules.students.domain.ParentRelation;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.ParentGuardianRepository;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private ParentGuardianRepository parentGuardianRepository;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private FeeService feeService;

	@Mock
	private StudentFeeAutoAssignmentService feeAutoAssignmentService;

	@Mock
	private HostelService hostelService;

	@Mock
	private TransportService transportService;

	@Mock
	private AuditLogService auditLogService;

	private StudentService studentService;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity sectionA;
	private SectionEntity sectionB;

	@BeforeEach
	void setUp() {
		studentService = new StudentService(
				studentRepository,
				parentGuardianRepository,
				studentClassAssignmentRepository,
				academicHierarchyService,
				feeService,
				feeAutoAssignmentService,
				hostelService,
				transportService,
				new StudentMapper(),
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		sectionA = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		sectionB = new SectionEntity(classEntity, "B", "Division B", 40, 2);
		setId(academicYear);
		setId(classEntity);
		setId(sectionA);
		setId(sectionB);
	}

	@Test
	void admitStudentCreatesProfileWithParentAssignmentAndDocuments() {
		when(studentRepository.existsByAdmissionNumberIgnoreCaseAndDeletedFalse("ADM-2026-0001")).thenReturn(false);
		mockHierarchy(sectionA);
		when(studentClassAssignmentRepository.existsActiveRollNumber(
				academicYear.getId(),
				classEntity.getId(),
				sectionA.getId(),
				"23",
				null))
				.thenReturn(false);
		when(parentGuardianRepository.findByEmailIgnoreCaseAndDeletedFalse("rajesh.sharma@example.com"))
				.thenReturn(Optional.empty());
		when(parentGuardianRepository.save(any(ParentGuardian.class))).thenAnswer(invocation -> {
			ParentGuardian parent = invocation.getArgument(0);
			setId(parent);
			return parent;
		});
		when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
			Student student = invocation.getArgument(0);
			setId(student);
			student.getParents().forEach(this::setId);
			student.getDocuments().forEach(this::setId);
			student.getClassAssignments().forEach(this::setId);
			return student;
		});

		StudentResponse response = studentService.admitStudent(admissionRequest(true));

		assertThat(response.admissionNumber()).isEqualTo("ADM-2026-0001");
		assertThat(response.parents()).hasSize(1);
		assertThat(response.parents().getFirst().primaryContact()).isTrue();
		assertThat(response.documents()).hasSize(1);
		assertThat(response.currentAssignment().className()).isEqualTo("Class 6");
		assertThat(response.currentAssignment().academicYearId()).isEqualTo(academicYear.getId());

		ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
		verify(studentRepository).saveAndFlush(studentCaptor.capture());
		Student savedStudent = studentCaptor.getValue();
		assertThat(savedStudent.getParents()).hasSize(1);
		assertThat(savedStudent.getDocuments()).hasSize(1);
		assertThat(savedStudent.getCurrentAssignment()).isPresent();
	}

	@Test
	void getParentsReturnsMappedParentGuardianDetails() {
		Student student = student();
		ParentGuardian parent = new ParentGuardian(
				"Rajesh",
				"Sharma",
				"rajesh.sharma@example.com",
				"+919812345678");
		setId(parent);
		student.addParent(parent, ParentRelation.FATHER, true, true, true);
		student.getParents().forEach(this::setId);
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));

		var response = studentService.getParents(student.getId());

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().displayName()).isEqualTo("Rajesh Sharma");
		assertThat(response.getFirst().relationType()).isEqualTo(ParentRelation.FATHER);
		assertThat(response.getFirst().phoneNumber()).isEqualTo("+919812345678");
		assertThat(response.getFirst().primaryContact()).isTrue();
	}

	@Test
	void addParentFlushesMappingBeforeReturningResponse() {
		Student student = student();
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(parentGuardianRepository.findByEmailIgnoreCaseAndDeletedFalse("rajesh.sharma@example.com"))
				.thenReturn(Optional.empty());
		when(parentGuardianRepository.save(any(ParentGuardian.class))).thenAnswer(invocation -> {
			ParentGuardian parent = invocation.getArgument(0);
			setId(parent);
			return parent;
		});
		when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(invocation -> {
			Student savedStudent = invocation.getArgument(0);
			savedStudent.getParents().forEach(this::setId);
			return savedStudent;
		});

		StudentResponse response = studentService.addParent(
				student.getId(),
				new ParentMappingRequest(
						ParentRelation.FATHER,
						true,
						true,
						true,
						parentRequest()));

		assertThat(response.parents()).hasSize(1);
		assertThat(response.parents().getFirst().mappingId()).isNotNull();
		assertThat(response.parents().getFirst().displayName()).isEqualTo("Rajesh Sharma");
	}

	@Test
	void admitStudentRejectsDuplicateAdmissionNumber() {
		when(studentRepository.existsByAdmissionNumberIgnoreCaseAndDeletedFalse("ADM-2026-0001")).thenReturn(true);

		assertThatThrownBy(() -> studentService.admitStudent(admissionRequest(true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);

		verify(studentRepository, never()).saveAndFlush(any());
	}

	@Test
	void admitStudentRequiresExactlyOnePrimaryParentContact() {
		assertThatThrownBy(() -> studentService.admitStudent(admissionRequest(false)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);

		verify(studentRepository, never()).existsByAdmissionNumberIgnoreCaseAndDeletedFalse(any());
	}

	@Test
	void assignClassSectionDeactivatesPreviousActiveAssignment() {
		UUID studentId = UUID.randomUUID();
		Student student = student();
		student.assignClassSection("2026-2027", "Class 6", "A", "23", LocalDate.of(2026, 4, 1));
		ReflectionTestUtils.setField(student, "id", studentId);
		when(studentRepository.findProfileByIdAndDeletedFalse(studentId)).thenReturn(Optional.of(student));
		mockHierarchy(sectionB);
		when(studentClassAssignmentRepository.existsActiveRollNumber(
				academicYear.getId(),
				classEntity.getId(),
				sectionB.getId(),
				"24",
				null))
				.thenReturn(false);

		StudentResponse response = studentService.assignClassSection(
				studentId,
				new ClassSectionAssignmentRequest(null, null, null, "2026-2027", "Class 6", "B", "24", LocalDate.of(2026, 5, 1)));

		assertThat(response.currentAssignment().sectionName()).isEqualTo("B");
		assertThat(student.getClassAssignments()).hasSize(2);
		assertThat(student.getClassAssignments().stream().filter(assignment -> assignment.isActive()).count()).isEqualTo(1);
		assertThat(student.getClassAssignments().stream()
				.filter(assignment -> !assignment.isActive())
				.findFirst())
				.hasValueSatisfying(assignment -> assertThat(assignment.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 4, 30)));
	}

	@Test
	void searchReturnsPaginatedStudentSummaries() {
		Student student = student();
		ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
		student.assignClassSection("2026-2027", "Class 6", "A", "23", LocalDate.of(2026, 4, 1));
		var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "admissionNumber"));
		when(studentRepository.findAll(
				ArgumentMatchers.<Specification<Student>>any(),
				any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(student), pageable, 1));

		PageResponse<StudentSummaryResponse> response = studentService.search(
				new StudentSearchRequest("aarav", StudentStatus.ACTIVE, null, null, null, null, "Class 6", "A", null, null),
				new PageRequestDto(0, 20, null, Sort.Direction.ASC));

		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.content().getFirst().className()).isEqualTo("Class 6");
		assertThat(response.content().getFirst().sectionName()).isEqualTo("A");
	}

	private StudentAdmissionRequest admissionRequest(boolean primaryContact) {
		return new StudentAdmissionRequest(
				"ADM-2026-0001",
				profileRequest(),
				StudentStatus.ACTIVE,
				List.of(new ParentMappingRequest(
						ParentRelation.FATHER,
						primaryContact,
						true,
						true,
						parentRequest())),
				new ClassSectionAssignmentRequest(null, null, null, "2026-2027", "Class 6", "A", "23", LocalDate.of(2026, 4, 1)),
				List.of(new StudentDocumentRequest(
						DocumentType.BIRTH_CERTIFICATE,
						"BC-2026-001",
						"birth-certificate.pdf",
						"application/pdf",
						240128L,
						"students/ADM-2026-0001/birth-certificate.pdf",
						null,
						"Original verified during admission")),
				null,
				null);
	}

	private Student student() {
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		student.updateProfile(
				"Aarav",
				"Kumar",
				"Sharma",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				"B+",
				"aarav.sharma@student.school.test",
				"+919876543210",
				LocalDate.of(2026, 4, 1),
				"Green Valley Kindergarten",
				"12 MG Road",
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India");
		return student;
	}

	private StudentProfileRequest profileRequest() {
		return new StudentProfileRequest(
				"Aarav",
				"Kumar",
				"Sharma",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				"B+",
				"aarav.sharma@student.school.test",
				"+919876543210",
				LocalDate.of(2026, 4, 1),
				"Green Valley Kindergarten",
				"12 MG Road",
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India");
	}

	private ParentGuardianRequest parentRequest() {
		return new ParentGuardianRequest(
				"Rajesh",
				"Sharma",
				"rajesh.sharma@example.com",
				"+919812345678",
				null,
				"Software Engineer",
				"12 MG Road",
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India",
				null);
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}

	private void mockHierarchy(SectionEntity section) {
		when(academicHierarchyService.resolveAcademicYear("2026-2027")).thenReturn(academicYear);
		when(academicHierarchyService.resolveClass(academicYear, "Class 6")).thenReturn(classEntity);
		when(academicHierarchyService.resolveSection(classEntity, section.getCode())).thenReturn(section);
	}
}
