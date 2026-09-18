package com.school.erp.modules.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.academic.infrastructure.ClassTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.SubjectTeacherMappingRepository;
import com.school.erp.modules.academic.infrastructure.TeacherRepository;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRecordRequest;
import com.school.erp.modules.attendance.api.dto.DailyAttendanceRequest;
import com.school.erp.modules.attendance.domain.AttendanceRecord;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.infrastructure.AttendanceRecordRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private AttendanceRecordRepository attendanceRecordRepository;

	@Mock
	private TeacherRepository teacherRepository;

	@Mock
	private ClassTeacherMappingRepository classTeacherMappingRepository;

	@Mock
	private SubjectTeacherMappingRepository subjectTeacherMappingRepository;

	@Mock
	private AuditLogService auditLogService;

	private AttendanceService attendanceService;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity section;
	private Student student;

	@BeforeEach
	void setUp() {
		attendanceService = new AttendanceService(
				academicHierarchyService,
				studentClassAssignmentRepository,
				studentRepository,
				attendanceRecordRepository,
				teacherRepository,
				classTeacherMappingRepository,
				subjectTeacherMappingRepository,
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		section = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		student = new Student("ADM-2026-0001", "Aarav", LocalDate.of(2014, 8, 17), Gender.MALE, LocalDate.of(2026, 4, 1));
		student.updateProfile(
				"Aarav",
				"Kumar",
				"Sharma",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				null,
				null,
				null,
				LocalDate.of(2026, 4, 1),
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		setId(academicYear);
		setId(classEntity);
		setId(section);
		setId(student);
		student.assignClassSection(academicYear, classEntity, section, "23", LocalDate.of(2026, 4, 1));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void getStudentsUsesAttendanceDateEffectiveRoster() {
		LocalDate attendanceDate = LocalDate.of(2026, 6, 10);
		StudentClassAssignment assignment = student.getCurrentAssignment().orElseThrow();
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
		when(studentClassAssignmentRepository.findEligibleByHierarchyOnDate(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				StudentStatus.INACTIVE))
				.thenReturn(List.of(assignment));

		var response = attendanceService.getStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate);

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().studentId()).isEqualTo(student.getId());
		assertThat(response.getFirst().rollNumber()).isEqualTo("23");
	}

	@Test
	void saveDailyRejectsStudentOutsideDateEffectiveRoster() {
		LocalDate attendanceDate = LocalDate.of(2026, 6, 10);
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
		when(studentClassAssignmentRepository.findEligibleByHierarchyOnDate(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				StudentStatus.INACTIVE))
				.thenReturn(List.of());

		DailyAttendanceRequest request = new DailyAttendanceRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				List.of(new DailyAttendanceRecordRequest(student.getId(), AttendanceStatus.PRESENT, null)));

		assertThatThrownBy(() -> attendanceService.saveDaily(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void saveDailyUpdatesExistingStudentDateRecordAcrossHierarchy() {
		LocalDate attendanceDate = LocalDate.of(2026, 6, 10);
		StudentClassAssignment assignment = student.getCurrentAssignment().orElseThrow();
		AttendanceRecord existing = record(attendanceDate, AttendanceStatus.ABSENT);
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
		when(studentClassAssignmentRepository.findEligibleByHierarchyOnDate(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				StudentStatus.INACTIVE))
				.thenReturn(List.of(assignment));
		when(attendanceRecordRepository.findByStudentIdsAndDate(List.of(student.getId()), attendanceDate))
				.thenReturn(List.of(existing));
		when(attendanceRecordRepository.findDailyRecords(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate))
				.thenReturn(List.of(existing));

		DailyAttendanceRequest request = new DailyAttendanceRequest(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				List.of(new DailyAttendanceRecordRequest(student.getId(), AttendanceStatus.PRESENT, "Recovered")));

		var response = attendanceService.saveDaily(request);

		assertThat(existing.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
		assertThat(existing.getRemarks()).isEqualTo("Recovered");
		assertThat(response.presentCount()).isEqualTo(1);
		verify(attendanceRecordRepository, never()).save(any(AttendanceRecord.class));
	}

	@Test
	void getClassSummaryReturnsEligibleCountAndSharedPercentage() {
		LocalDate fromDate = LocalDate.of(2026, 6, 1);
		LocalDate toDate = LocalDate.of(2026, 6, 30);
		List<AttendanceRecord> records = List.of(
				record(LocalDate.of(2026, 6, 1), AttendanceStatus.PRESENT),
				record(LocalDate.of(2026, 6, 2), AttendanceStatus.LATE),
				record(LocalDate.of(2026, 6, 3), AttendanceStatus.HALF_DAY),
				record(LocalDate.of(2026, 6, 4), AttendanceStatus.ABSENT),
				record(LocalDate.of(2026, 6, 5), AttendanceStatus.LEAVE));
		stubHierarchy();
		when(attendanceRecordRepository.findClassSummaryRecords(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				fromDate,
				toDate))
				.thenReturn(records);
		when(studentClassAssignmentRepository.countEligibleStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				toDate,
				StudentStatus.INACTIVE))
				.thenReturn(5L);

		var response = attendanceService.getClassSummary(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				fromDate,
				toDate);

		assertThat(response.academicYearId()).isEqualTo(academicYear.getId());
		assertThat(response.classId()).isEqualTo(classEntity.getId());
		assertThat(response.sectionId()).isEqualTo(section.getId());
		assertThat(response.eligibleStudentCount()).isEqualTo(5);
		assertThat(response.totalRecords()).isEqualTo(5);
		assertThat(response.presentCount()).isEqualTo(1);
		assertThat(response.lateCount()).isEqualTo(1);
		assertThat(response.halfDayCount()).isEqualTo(1);
		assertThat(response.absentCount()).isEqualTo(1);
		assertThat(response.leaveCount()).isEqualTo(1);
		assertThat(response.attendancePercentage()).isEqualByComparingTo("50.00");
	}

	@Test
	void getMonthlySummaryUsesCalendarMonthAndEligibleCountAtMonthEnd() {
		List<AttendanceRecord> records = List.of(
				record(LocalDate.of(2026, 2, 3), AttendanceStatus.PRESENT),
				record(LocalDate.of(2026, 2, 4), AttendanceStatus.HALF_DAY));
		stubHierarchy();
		when(attendanceRecordRepository.findClassSummaryRecords(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				LocalDate.of(2026, 2, 1),
				LocalDate.of(2026, 2, 28)))
				.thenReturn(records);
		when(studentClassAssignmentRepository.countEligibleStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				LocalDate.of(2026, 2, 28),
				StudentStatus.INACTIVE))
				.thenReturn(2L);

		var response = attendanceService.getMonthlySummary(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				2026,
				2);

		assertThat(response.fromDate()).isEqualTo(LocalDate.of(2026, 2, 1));
		assertThat(response.toDate()).isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(response.eligibleStudentCount()).isEqualTo(2);
		assertThat(response.attendancePercentage()).isEqualByComparingTo("75.00");
	}

	@Test
	void getClassSummaryReturnsZeroPercentageWhenThereAreNoRecords() {
		LocalDate fromDate = LocalDate.of(2026, 7, 1);
		LocalDate toDate = LocalDate.of(2026, 7, 31);
		stubHierarchy();
		when(attendanceRecordRepository.findClassSummaryRecords(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				fromDate,
				toDate))
				.thenReturn(List.of());
		when(studentClassAssignmentRepository.countEligibleStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				toDate,
				StudentStatus.INACTIVE))
				.thenReturn(3L);

		var response = attendanceService.getClassSummary(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				fromDate,
				toDate);

		assertThat(response.eligibleStudentCount()).isEqualTo(3);
		assertThat(response.totalRecords()).isZero();
		assertThat(response.attendancePercentage()).isZero();
	}

	@Test
	void getStudentAttendanceHistoryUsesStoredHistoricalClassContext() {
		AcademicYear oldYear = new AcademicYear("AY-2025-26", "2025-2026", LocalDate.of(2025, 4, 1), LocalDate.of(2026, 3, 31));
		ClassEntity oldClass = new ClassEntity(oldYear, "CLASS-4", "Class 4", 4);
		SectionEntity oldSection = new SectionEntity(oldClass, "A", "Division A", 40, 1);
		setId(oldYear);
		setId(oldClass);
		setId(oldSection);
		AttendanceRecord historicalRecord = record(oldYear, oldClass, oldSection, LocalDate.of(2025, 8, 10), AttendanceStatus.PRESENT);
		when(academicHierarchyService.loadAcademicYear(oldYear.getId())).thenReturn(oldYear);
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(attendanceRecordRepository.findStudentHistory(
				eq(student.getId()),
				eq(oldYear.getId()),
				isNull(),
				isNull(),
				isNull(),
				any()))
				.thenReturn(new PageImpl<>(List.of(historicalRecord)));
		when(attendanceRecordRepository.findStudentHistoryForSummary(
				student.getId(),
				oldYear.getId(),
				null,
				null,
				null))
				.thenReturn(List.of(historicalRecord));

		var response = attendanceService.getStudentAttendanceHistory(
				student.getId(),
				oldYear.getId(),
				null,
				null,
				null,
				new PageRequestDto(0, 20, null, Sort.Direction.ASC),
				null);

		assertThat(response.academicYear()).isEqualTo("2025-2026");
		assertThat(response.className()).isEqualTo("Class 4");
		assertThat(response.sectionName()).isEqualTo("Division A");
	}

	@Test
	void teacherCanLoadAssignedClassAttendanceScope() {
		UUID userAccountId = UUID.randomUUID();
		Teacher teacher = teacher(userAccountId);
		LocalDate attendanceDate = LocalDate.of(2026, 6, 10);
		StudentClassAssignment assignment = student.getCurrentAssignment().orElseThrow();
		authenticateTeacher(userAccountId);
		stubHierarchy();
		when(teacherRepository.findByUserAccountIdAndDeletedFalse(userAccountId)).thenReturn(Optional.of(teacher));
		when(classTeacherMappingRepository.existsByTeacherIdAndClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(
				teacher.getId(),
				classEntity.getId(),
				section.getId()))
				.thenReturn(true);
		when(studentClassAssignmentRepository.findEligibleByHierarchyOnDate(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate,
				StudentStatus.INACTIVE))
				.thenReturn(List.of(assignment));

		var response = attendanceService.getStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				attendanceDate);

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().studentId()).isEqualTo(student.getId());
	}

	@Test
	void teacherCannotLoadUnassignedClassAttendanceScope() {
		UUID userAccountId = UUID.randomUUID();
		Teacher teacher = teacher(userAccountId);
		authenticateTeacher(userAccountId);
		stubHierarchy();
		when(teacherRepository.findByUserAccountIdAndDeletedFalse(userAccountId)).thenReturn(Optional.of(teacher));
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

		assertThatThrownBy(() -> attendanceService.getStudents(
				academicYear.getId(),
				classEntity.getId(),
				section.getId(),
				LocalDate.of(2026, 6, 10)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void getStudentAttendanceHistoryCalculatesSummaryFromFilteredRecords() {
		List<AttendanceRecord> records = List.of(
				record(LocalDate.of(2026, 6, 1), AttendanceStatus.PRESENT),
				record(LocalDate.of(2026, 6, 2), AttendanceStatus.LATE),
				record(LocalDate.of(2026, 6, 3), AttendanceStatus.HALF_DAY),
				record(LocalDate.of(2026, 6, 4), AttendanceStatus.ABSENT));
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(studentRepository.findProfileByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(attendanceRecordRepository.findStudentHistory(
				eq(student.getId()),
				eq(academicYear.getId()),
				isNull(),
				isNull(),
				isNull(),
				any()))
				.thenReturn(new PageImpl<>(records));
		when(attendanceRecordRepository.findStudentHistoryForSummary(
				student.getId(),
				academicYear.getId(),
				null,
				null,
				null))
				.thenReturn(records);

		var response = attendanceService.getStudentAttendanceHistory(
				student.getId(),
				academicYear.getId(),
				null,
				null,
				null,
				new PageRequestDto(0, 20, null, Sort.Direction.ASC),
				null);

		assertThat(response.studentName()).isEqualTo("Aarav Kumar Sharma");
		assertThat(response.academicYear()).isEqualTo("2026-2027");
		assertThat(response.className()).isEqualTo("Class 6");
		assertThat(response.sectionName()).isEqualTo("Division A");
		assertThat(response.totalWorkingDays()).isEqualTo(4);
		assertThat(response.presentDays()).isEqualTo(1);
		assertThat(response.lateDays()).isEqualTo(1);
		assertThat(response.halfDays()).isEqualTo(1);
		assertThat(response.absentDays()).isEqualTo(1);
		assertThat(response.attendancePercentage()).isEqualByComparingTo("62.50");
		assertThat(response.attendanceRecords().content()).hasSize(4);
		assertThat(response.attendanceRecords().content().getFirst().markedBy()).isEqualTo("teacher@school.test");
	}

	@Test
	void getStudentAttendanceHistoryRejectsInvalidDateRange() {
		assertThatThrownBy(() -> attendanceService.getStudentAttendanceHistory(
				student.getId(),
				null,
				LocalDate.of(2026, 6, 5),
				LocalDate.of(2026, 6, 1),
				null,
				new PageRequestDto(0, 20, null, Sort.Direction.ASC),
				null))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	private AttendanceRecord record(LocalDate date, AttendanceStatus status) {
		return record(academicYear, classEntity, section, date, status);
	}

	private AttendanceRecord record(
			AcademicYear academicYear,
			ClassEntity classEntity,
			SectionEntity section,
			LocalDate date,
			AttendanceStatus status) {
		AttendanceRecord record = new AttendanceRecord(
				academicYear,
				classEntity,
				section,
				student,
				date,
				status,
				status.name().toLowerCase());
		setId(record);
		ReflectionTestUtils.setField(record, "createdBy", "teacher@school.test");
		ReflectionTestUtils.setField(record, "updatedBy", "teacher@school.test");
		ReflectionTestUtils.setField(record, "createdAt", Instant.parse("2026-06-01T08:00:00Z"));
		ReflectionTestUtils.setField(record, "updatedAt", Instant.parse("2026-06-01T08:05:00Z"));
		return record;
	}

	private void stubHierarchy() {
		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(academicHierarchyService.loadSectionForClass(classEntity.getId(), section.getId())).thenReturn(section);
	}

	private Teacher teacher(UUID userAccountId) {
		Teacher teacher = new Teacher("T-001", "Nisha", "Patel", "nisha.patel@school.test", "9890000002");
		teacher.updateProfile(
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
		setId(teacher);
		return teacher;
	}

	private void authenticateTeacher(UUID userAccountId) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				userAccountId.toString(),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
