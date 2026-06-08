package com.school.erp.modules.attendance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.school.erp.modules.attendance.domain.AttendanceRecord;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.infrastructure.AttendanceRecordRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
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

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
