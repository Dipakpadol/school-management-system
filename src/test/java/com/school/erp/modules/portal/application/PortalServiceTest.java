package com.school.erp.modules.portal.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.attendance.api.dto.StudentAttendanceHistoryResponse;
import com.school.erp.modules.attendance.application.AttendanceService;
import com.school.erp.modules.communications.application.CommunicationMapper;
import com.school.erp.modules.communications.infrastructure.CommunicationRecordRepository;
import com.school.erp.modules.exams.api.dto.ExamStudentResponse;
import com.school.erp.modules.exams.api.dto.MarksEntryRecordRequest;
import com.school.erp.modules.exams.api.dto.MarksEntryRequest;
import com.school.erp.modules.exams.application.ExamService;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.hostel.application.HostelService;
import com.school.erp.modules.library.application.LibraryService;
import com.school.erp.modules.library.infrastructure.LibraryMembershipRepository;
import com.school.erp.modules.students.application.StudentMapper;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.teachers.application.TeacherMapper;
import com.school.erp.modules.teachers.infrastructure.TeacherAcademicAssignmentRepository;
import com.school.erp.modules.transport.application.TransportService;

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
class PortalServiceTest {

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private AttendanceService attendanceService;

	@Mock
	private FeeService feeService;

	@Mock
	private ExamService examService;

	@Mock
	private HostelService hostelService;

	@Mock
	private TransportService transportService;

	@Mock
	private LibraryService libraryService;

	@Mock
	private LibraryMembershipRepository libraryMembershipRepository;

	@Mock
	private CommunicationRecordRepository communicationRecordRepository;

	@Mock
	private TeacherRepository teacherRepository;

	@Mock
	private TeacherAcademicAssignmentRepository teacherAcademicAssignmentRepository;

	@Mock
	private ClassTeacherMappingRepository classTeacherMappingRepository;

	@Mock
	private SubjectTeacherMappingRepository subjectTeacherMappingRepository;

	private PortalService portalService;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity section;
	private Student student;
	private Teacher teacher;

	@BeforeEach
	void setUp() {
		portalService = new PortalService(
				studentRepository,
				new StudentMapper(),
				attendanceService,
				feeService,
				examService,
				hostelService,
				transportService,
				libraryService,
				libraryMembershipRepository,
				communicationRecordRepository,
				new CommunicationMapper(),
				teacherRepository,
				new TeacherMapper(),
				teacherAcademicAssignmentRepository,
				classTeacherMappingRepository,
				subjectTeacherMappingRepository);
		academicYear = entity(new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)));
		classEntity = entity(new ClassEntity(academicYear, "CLASS-6", "Class 6", 6));
		section = entity(new SectionEntity(classEntity, "A", "Division A", 40, 1));
		student = entity(new Student("ADM-001", "Aarav", LocalDate.of(2014, 8, 17), Gender.MALE, LocalDate.of(2026, 4, 1)));
		student.updateProfile(
				"Aarav",
				null,
				"Sharma",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				null,
				"student@school.test",
				"9890000001",
				LocalDate.of(2026, 4, 1),
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		student.assignClassSection(academicYear, classEntity, section, "6A-01", LocalDate.of(2026, 4, 1));
		student.getClassAssignments().forEach(this::setId);
		teacher = teacher(UUID.randomUUID());
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void studentProfileUsesAuthenticatedStudentIdentity() {
		UUID userId = UUID.randomUUID();
		student.linkUserAccount(userId);
		authenticate(userId, "ROLE_STUDENT");
		when(studentRepository.findByUserAccountIdAndDeletedFalse(userId)).thenReturn(Optional.of(student));
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));

		var response = portalService.studentProfile();

		assertThat(response.id()).isEqualTo(student.getId());
		assertThat(response.admissionNumber()).isEqualTo("ADM-001");
		assertThat(response.currentAssignment().className()).isEqualTo("Class 6");
	}

	@Test
	void studentProfileRejectsUnlinkedStudentAccount() {
		UUID userId = UUID.randomUUID();
		authenticate(userId, "ROLE_STUDENT");
		when(studentRepository.findByUserAccountIdAndDeletedFalse(userId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> portalService.studentProfile())
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void parentChildAttendanceAllowsOnlyLinkedChild() {
		UUID parentUserId = UUID.randomUUID();
		authenticate(parentUserId, "ROLE_PARENT");
		StudentAttendanceHistoryResponse attendance = attendanceResponse(student.getId());
		when(studentRepository.findChildrenByParentUserAccountId(parentUserId)).thenReturn(List.of(student));
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(attendanceService.getStudentAttendanceHistory(
				eq(student.getId()),
				eq(academicYear.getId()),
				isNull(),
				isNull(),
				isNull(),
				any(PageRequestDto.class),
				isNull()))
				.thenReturn(attendance);

		var response = portalService.parentChildAttendance(
				student.getId(),
				academicYear.getId(),
				null,
				null,
				null,
				new PageRequestDto(0, 20, null, null),
				null);

		assertThat(response.studentId()).isEqualTo(student.getId());
		verify(attendanceService).getStudentAttendanceHistory(
				eq(student.getId()),
				eq(academicYear.getId()),
				isNull(),
				isNull(),
				isNull(),
				any(PageRequestDto.class),
				isNull());
	}

	@Test
	void parentChildAttendanceRejectsUnlinkedChild() {
		UUID parentUserId = UUID.randomUUID();
		UUID requestedChildId = UUID.randomUUID();
		authenticate(parentUserId, "ROLE_PARENT");
		when(studentRepository.findChildrenByParentUserAccountId(parentUserId)).thenReturn(List.of(student));

		assertThatThrownBy(() -> portalService.parentChildAttendance(
				requestedChildId,
				academicYear.getId(),
				null,
				null,
				null,
				new PageRequestDto(0, 20, null, null),
				null))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(attendanceService, never()).getStudentAttendanceHistory(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void teacherStudentsAllowsAssignedClassScope() {
		UUID userId = teacher.getUserAccountId();
		authenticate(userId, "ROLE_TEACHER");
		List<ExamStudentResponse> roster = List.of(new ExamStudentResponse(student.getId(), "ADM-001", "6A-01", "Aarav Sharma"));
		when(teacherRepository.findByUserAccountIdAndDeletedFalse(userId)).thenReturn(Optional.of(teacher));
		when(classTeacherMappingRepository.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				teacher.getId(),
				classEntity.getId(),
				section.getId()))
				.thenReturn(true);
		when(examService.getStudents(academicYear.getId(), classEntity.getId(), section.getId())).thenReturn(roster);

		var response = portalService.teacherStudents(academicYear.getId(), classEntity.getId(), section.getId());

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().studentId()).isEqualTo(student.getId());
	}

	@Test
	void teacherStudentsRejectsUnassignedClassScope() {
		UUID userId = teacher.getUserAccountId();
		authenticate(userId, "ROLE_TEACHER");
		when(teacherRepository.findByUserAccountIdAndDeletedFalse(userId)).thenReturn(Optional.of(teacher));
		when(classTeacherMappingRepository.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				teacher.getId(),
				classEntity.getId(),
				section.getId()))
				.thenReturn(false);
		when(subjectTeacherMappingRepository.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				teacher.getId(),
				classEntity.getId(),
				section.getId()))
				.thenReturn(false);
		when(teacherAcademicAssignmentRepository.findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(
				teacher.getId(),
				academicYear.getId()))
				.thenReturn(List.of());

		assertThatThrownBy(() -> portalService.teacherStudents(academicYear.getId(), classEntity.getId(), section.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(examService, never()).getStudents(any(), any(), any());
	}

	@Test
	void teacherSaveMarksRejectsUnassignedSubjectScope() {
		UUID userId = teacher.getUserAccountId();
		UUID subjectId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		authenticate(userId, "ROLE_TEACHER");
		when(teacherRepository.findByUserAccountIdAndDeletedFalse(userId)).thenReturn(Optional.of(teacher));
		when(subjectTeacherMappingRepository.existsByTeacherIdAndClassEntityIdAndSectionIdAndSubjectIdAndActiveTrueAndDeletedFalse(
				teacher.getId(),
				classEntity.getId(),
				section.getId(),
				subjectId))
				.thenReturn(false);
		when(teacherAcademicAssignmentRepository.findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(
				teacher.getId(),
				academicYear.getId()))
				.thenReturn(List.of());

		MarksEntryRequest request = new MarksEntryRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				scheduleId,
				subjectId,
				List.of(new MarksEntryRecordRequest(student.getId(), new BigDecimal("82.00"), null, null)));

		assertThatThrownBy(() -> portalService.teacherSaveMarks(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
		verify(examService, never()).saveMarks(any());
	}

	private StudentAttendanceHistoryResponse attendanceResponse(UUID studentId) {
		return new StudentAttendanceHistoryResponse(
				studentId,
				"Aarav Sharma",
				academicYear.getId(),
				academicYear.getName(),
				classEntity.getId(),
				classEntity.getName(),
				section.getId(),
				section.getName(),
				0,
				0,
				0,
				0,
				0,
				0,
				BigDecimal.ZERO,
				new PageResponse<>(List.of(), 0, 20, 0, 0, true, true));
	}

	private Teacher teacher(UUID userAccountId) {
		Teacher value = new Teacher("T-001", "Nisha", "Patel", "nisha.patel@school.test", "9890000002");
		value.updateProfile(
				"T-001",
				"Nisha",
				null,
				"Patel",
				Gender.FEMALE,
				LocalDate.of(1991, 5, 10),
				"9890000002",
				"nisha.patel@school.test",
				"M.Ed",
				8,
				LocalDate.of(2021, 6, 1),
				null,
				userAccountId);
		return entity(value);
	}

	private void authenticate(UUID userId, String roleAuthority) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				userId.toString(),
				null,
				List.of(new SimpleGrantedAuthority(roleAuthority))));
	}

	private <T> T entity(T entity) {
		setId(entity);
		return entity;
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
