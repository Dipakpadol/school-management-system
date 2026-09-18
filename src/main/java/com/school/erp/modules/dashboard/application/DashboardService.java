package com.school.erp.modules.dashboard.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.List;

import com.school.erp.common.audit.domain.AuditLog;
import com.school.erp.common.audit.infrastructure.AuditLogRepository;
import com.school.erp.modules.attendance.application.AttendanceSummaryCalculator;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.attendance.infrastructure.AttendanceRecordRepository;
import com.school.erp.modules.dashboard.api.dto.DashboardSummaryResponse;
import com.school.erp.modules.dashboard.api.dto.DashboardSummaryResponse.BirthdayTodayResponse;
import com.school.erp.modules.dashboard.api.dto.DashboardSummaryResponse.RecentActivityResponse;
import com.school.erp.modules.dashboard.api.dto.TodayAttendanceResponse;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.infrastructure.FeeReportTotals;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;
import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.infrastructure.LibraryBookCopyRepository;
import com.school.erp.modules.library.infrastructure.LibraryBookRepository;
import com.school.erp.modules.library.infrastructure.LibraryFineRepository;
import com.school.erp.modules.library.infrastructure.LibraryLoanRepository;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.ParentGuardianRepository;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Kolkata");

	private final StudentRepository studentRepository;
	private final ParentGuardianRepository parentGuardianRepository;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final AttendanceRecordRepository attendanceRecordRepository;
	private final UserAccountRepository userAccountRepository;
	private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
	private final StaffRepository staffRepository;
	private final LibraryBookRepository libraryBookRepository;
	private final LibraryBookCopyRepository libraryBookCopyRepository;
	private final LibraryLoanRepository libraryLoanRepository;
	private final LibraryFineRepository libraryFineRepository;
	private final AuditLogRepository auditLogRepository;

	public DashboardSummaryResponse summary() {
		long totalStudents = safeLong("total students", studentRepository::countByDeletedFalse);
		long totalUsers = safeLong("total users", userAccountRepository::countByDeletedFalse);
		long activeUsers = safeLong("active users", () -> userAccountRepository.countByStatusAndDeletedFalse(UserStatus.ACTIVE));
		long inactiveUsers = Math.max(0, totalUsers - activeUsers);
		long totalStaff = safeLong("active staff", () -> staffRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE));
		long totalTeachers = safeLong(
				"teaching staff",
				() -> staffRepository.countByStaffTypeAndStatusAndDeletedFalse(StaffType.TEACHING, EmploymentStatus.ACTIVE));
		long nonTeachingStaff = safeLong(
				"non-teaching staff",
				() -> staffRepository.countByStaffTypeAndStatusAndDeletedFalse(StaffType.NON_TEACHING, EmploymentStatus.ACTIVE));
		long guardianCount = safeLong("parent guardians", parentGuardianRepository::countByDeletedFalse);
		long parentUserCount = safeLong(
				"parent users",
				() -> userAccountRepository.countByRoleNamesAndDeletedFalse(List.of(RoleName.PARENT.name())));

		FeeReportTotals feeTotals = safeValue(
				"fee totals",
				() -> studentFeeAssignmentRepository.summarizeAll(FeeAssignmentStatus.CANCELLED),
				null);
		TodayAttendanceResponse todayAttendance = todayAttendance(null, null, null);
		long totalLibraryBooks = safeLong("library books", libraryBookRepository::countByDeletedFalse);
		long availableLibraryCopies = safeLong(
				"available library copies",
				() -> libraryBookCopyRepository.countByStatusAndDeletedFalse(LibraryBookCopyStatus.AVAILABLE));
		long libraryOverdueLoans = safeLong(
				"library overdue loans",
				() -> libraryLoanRepository.countByStatusAndDueDateBeforeAndDeletedFalse(LibraryLoanStatus.ACTIVE, LocalDate.now(SCHOOL_ZONE)));
		BigDecimal pendingLibraryFineAmount = safeValue(
				"library pending fines",
				libraryFineRepository::pendingAmount,
				ZERO);

		return new DashboardSummaryResponse(
				totalStudents,
				totalStaff,
				totalTeachers,
				nonTeachingStaff,
				Math.max(guardianCount, parentUserCount),
				totalUsers,
				activeUsers,
				inactiveUsers,
				todayAttendance.attendancePercentage(),
				feeTotals == null ? ZERO : nullToZero(feeTotals.getPaidAmount()),
				feeTotals == null ? ZERO : nullToZero(feeTotals.getBalanceAmount()),
				totalLibraryBooks,
				availableLibraryCopies,
				libraryOverdueLoans,
				nullToZero(pendingLibraryFineAmount),
				recentActivities(),
				List.of(),
				birthdaysToday());
	}

	public TodayAttendanceResponse todayAttendance(UUID academicYearId, UUID classId, UUID sectionId) {
		LocalDate today = LocalDate.now(SCHOOL_ZONE);
		var records = safeValue(
				"today attendance records",
				() -> attendanceRecordRepository.findTodayDashboardRecords(today, academicYearId, classId, sectionId),
				List.<com.school.erp.modules.attendance.domain.AttendanceRecord>of());
		long totalStudents = safeLong(
				"active students for today attendance",
				() -> studentClassAssignmentRepository.countEligibleStudents(
						academicYearId,
						classId,
						sectionId,
						today,
						StudentStatus.INACTIVE));
		long present = count(records, AttendanceStatus.PRESENT);
		long absent = count(records, AttendanceStatus.ABSENT);
		long late = count(records, AttendanceStatus.LATE);
		long halfDay = count(records, AttendanceStatus.HALF_DAY);
		long leave = count(records, AttendanceStatus.LEAVE);
		return new TodayAttendanceResponse(
				today,
				totalStudents,
				present,
				absent,
				late,
				halfDay,
				leave,
				AttendanceSummaryCalculator.percentage(totalStudents, present, late, halfDay),
				List.of());
	}

	private List<RecentActivityResponse> recentActivities() {
		return safeValue(
				"recent activities",
				() -> auditLogRepository
						.findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "performedAt")))
						.stream()
						.map(this::toRecentActivity)
						.toList(),
				List.of());
	}

	private List<BirthdayTodayResponse> birthdaysToday() {
		LocalDate today = LocalDate.now();
		return safeValue(
				"today birthdays",
				() -> studentRepository
						.findBirthdaysByMonthAndDay(today.getMonthValue(), today.getDayOfMonth(), PageRequest.of(0, 8))
						.stream()
						.map(this::toBirthday)
						.toList(),
				List.of());
	}

	private RecentActivityResponse toRecentActivity(AuditLog auditLog) {
		return new RecentActivityResponse(
				auditLog.getId(),
				auditLog.getModuleName(),
				auditLog.getEntityName(),
				auditLog.getEntityId(),
				auditLog.getAction(),
				auditLog.getPerformedBy(),
				auditLog.getPerformedAt());
	}

	private BirthdayTodayResponse toBirthday(Student student) {
		var assignment = student.getCurrentAssignment().orElse(null);
		return new BirthdayTodayResponse(
				student.getId(),
				student.getDisplayName(),
				student.getDateOfBirth(),
				assignment == null ? null : assignment.getClassName(),
				assignment == null ? null : assignment.getSectionName());
	}

	private long safeLong(String metricName, Supplier<Long> supplier) {
		return safeValue(metricName, supplier, 0L);
	}

	private <T> T safeValue(String metricName, Supplier<T> supplier, T fallback) {
		try {
			T value = supplier.get();
			return value == null ? fallback : value;
		}
		catch (RuntimeException ex) {
			log.warn(
					"Dashboard metric '{}' unavailable ({}: {}). Returning fallback.",
					metricName,
					ex.getClass().getSimpleName(),
					ex.getMessage());
			return fallback;
		}
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? ZERO : value;
	}

	private long count(
			List<com.school.erp.modules.attendance.domain.AttendanceRecord> records,
			AttendanceStatus status) {
		return records.stream().filter(record -> record.getStatus() == status).count();
	}

}
