package com.school.erp.modules.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.audit.domain.AuditLog;
import com.school.erp.common.audit.infrastructure.AuditLogRepository;
import com.school.erp.modules.attendance.domain.AttendanceRecord;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.infrastructure.AttendanceRecordRepository;
import com.school.erp.modules.fees.infrastructure.FeeReportTotals;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.ParentGuardianRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private ParentGuardianRepository parentGuardianRepository;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private AttendanceRecordRepository attendanceRecordRepository;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private StudentFeeAssignmentRepository assignmentRepository;

	@Mock
	private AuditLogRepository auditLogRepository;

	@Mock
	private FeeReportTotals feeReportTotals;

	private DashboardService dashboardService;

	@BeforeEach
	void setUp() {
		dashboardService = new DashboardService(
				studentRepository,
				parentGuardianRepository,
				studentClassAssignmentRepository,
				attendanceRecordRepository,
				userAccountRepository,
				assignmentRepository,
				auditLogRepository);
	}

	@Test
	void summaryReturnsLiveCountsFeesActivitiesAndBirthdays() {
		when(studentRepository.countByDeletedFalse()).thenReturn(3L);
		when(parentGuardianRepository.countByDeletedFalse()).thenReturn(2L);
		when(userAccountRepository.countByDeletedFalse()).thenReturn(7L);
		when(userAccountRepository.countByStatusAndDeletedFalse(UserStatus.ACTIVE)).thenReturn(6L);
		when(userAccountRepository.countByRoleNamesAndDeletedFalse(List.of(RoleName.TEACHER))).thenReturn(1L);
		when(userAccountRepository.countByRoleNamesAndDeletedFalse(List.of(RoleName.PARENT))).thenReturn(1L);
		when(userAccountRepository.countByRoleNamesAndDeletedFalse(List.of(
				RoleName.SUPER_ADMIN,
				RoleName.ADMIN,
				RoleName.PRINCIPAL,
				RoleName.TEACHER,
				RoleName.ACCOUNTANT,
				RoleName.RECEPTIONIST,
				RoleName.WARDEN))).thenReturn(5L);
		when(assignmentRepository.summarizeAll()).thenReturn(feeReportTotals);
		when(feeReportTotals.getPaidAmount()).thenReturn(new BigDecimal("18000.00"));
		when(feeReportTotals.getBalanceAmount()).thenReturn(new BigDecimal("60000.00"));
		when(attendanceRecordRepository.findTodayDashboardRecords(any(LocalDate.class), isNull(), isNull(), isNull()))
				.thenReturn(List.of(
						attendanceRecord(AttendanceStatus.PRESENT),
						attendanceRecord(AttendanceStatus.PRESENT),
						attendanceRecord(AttendanceStatus.ABSENT),
						attendanceRecord(AttendanceStatus.LATE)));
		when(studentClassAssignmentRepository.countActiveStudents(isNull(), isNull(), isNull())).thenReturn(4L);
		when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(auditLog())));
		when(studentRepository.findBirthdaysByMonthAndDay(anyInt(), anyInt(), any(Pageable.class)))
				.thenReturn(List.of(studentWithBirthday()));

		var response = dashboardService.summary();

		assertThat(response.totalStudents()).isEqualTo(3);
		assertThat(response.totalStaff()).isEqualTo(5);
		assertThat(response.totalTeachers()).isEqualTo(1);
		assertThat(response.totalParents()).isEqualTo(2);
		assertThat(response.totalUsers()).isEqualTo(7);
		assertThat(response.activeUsers()).isEqualTo(6);
		assertThat(response.inactiveUsers()).isEqualTo(1);
		assertThat(response.todayAttendancePercentage()).isEqualByComparingTo("50.00");
		assertThat(response.totalFeeCollected()).isEqualByComparingTo("18000.00");
		assertThat(response.pendingFeeAmount()).isEqualByComparingTo("60000.00");
		assertThat(response.recentActivities()).hasSize(1);
		assertThat(response.birthdaysToday()).hasSize(1);
	}

	@Test
	void summaryFallsBackWhenOptionalDashboardDataIsUnavailable() {
		when(studentRepository.countByDeletedFalse()).thenThrow(new IllegalStateException("students table missing"));
		when(attendanceRecordRepository.findTodayDashboardRecords(any(LocalDate.class), isNull(), isNull(), isNull()))
				.thenThrow(new IllegalStateException("attendance table missing"));
		when(studentClassAssignmentRepository.countActiveStudents(isNull(), isNull(), isNull()))
				.thenThrow(new IllegalStateException("assignments table missing"));
		when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
		when(studentRepository.findBirthdaysByMonthAndDay(anyInt(), anyInt(), any(Pageable.class))).thenReturn(List.of());

		var response = dashboardService.summary();

		assertThat(response.totalStudents()).isZero();
		assertThat(response.todayAttendancePercentage()).isZero();
		assertThat(response.totalFeeCollected()).isZero();
		assertThat(response.pendingFeeAmount()).isZero();
		assertThat(response.recentActivities()).isEmpty();
		assertThat(response.notifications()).isEmpty();
		assertThat(response.birthdaysToday()).isEmpty();
	}

	private AuditLog auditLog() {
		AuditLog auditLog = new AuditLog(
				"FEES",
				"StudentFeeAssignment",
				"assignment-1",
				"PAYMENT_COLLECTED",
				null,
				null,
				"accountant@school.com",
				Instant.parse("2026-05-24T08:00:00Z"),
				"127.0.0.1");
		ReflectionTestUtils.setField(auditLog, "id", UUID.randomUUID());
		return auditLog;
	}

	private AttendanceRecord attendanceRecord(AttendanceStatus status) {
		return new AttendanceRecord(null, null, null, null, LocalDate.now(), status, null);
	}

	private Student studentWithBirthday() {
		Student student = new Student(
				"ADM-DEMO-0001",
				"Aarav",
				LocalDate.now().minusYears(12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		student.updateProfile(
				"Aarav",
				null,
				"Sharma",
				LocalDate.now().minusYears(12),
				Gender.MALE,
				"B+",
				"aarav.sharma@student.school.test",
				"9890000001",
				LocalDate.of(2026, 4, 1),
				null,
				null,
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India");
		student.assignClassSection("2026-2027", "Class 6", "A", "6A-01", LocalDate.of(2026, 4, 1));
		ReflectionTestUtils.setField(student, "id", UUID.randomUUID());
		return student;
	}
}
