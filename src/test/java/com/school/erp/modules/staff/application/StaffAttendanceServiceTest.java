package com.school.erp.modules.staff.application;

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

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.staff.api.dto.StaffAttendanceDailyRequest;
import com.school.erp.modules.staff.api.dto.StaffAttendanceEntryRequest;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveType;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffAttendance;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.StaffAttendanceRepository;
import com.school.erp.modules.staff.infrastructure.StaffLeaveRequestRepository;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.Gender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffAttendanceServiceTest {

	@Mock
	private StaffRepository staffRepository;

	@Mock
	private StaffAttendanceRepository attendanceRepository;

	@Mock
	private StaffLeaveRequestRepository leaveRequestRepository;

	@Mock
	private StaffService staffService;

	@Mock
	private AuditLogService auditLogService;

	private StaffAttendanceService attendanceService;
	private Staff staff;
	private Department department;
	private Designation designation;

	@BeforeEach
	void setUp() {
		attendanceService = new StaffAttendanceService(
				staffRepository,
				attendanceRepository,
				leaveRequestRepository,
				staffService,
				new StaffMapper(),
				auditLogService);
		department = new Department("Administration", null, true);
		designation = new Designation("Office Assistant", department, null, true);
		setId(department);
		setId(designation);
		staff = staff("EMP-010");
	}

	@Test
	void dailyRosterMarksApprovedLeaveRowsAsLeave() {
		LocalDate date = LocalDate.of(2026, 8, 3);
		StaffLeaveRequest leave = leave(date, date);
		when(staffRepository.findRoster(date, department.getId(), null)).thenReturn(List.of(staff));
		when(attendanceRepository.findByStaffIdInAndAttendanceDateAndDeletedFalse(List.of(staff.getId()), date))
				.thenReturn(List.of());
		when(leaveRequestRepository.findApprovedCoveringDate(List.of(staff.getId()), date)).thenReturn(List.of(leave));

		var response = attendanceService.daily(date, department.getId(), null);

		assertThat(response.totalStaff()).isEqualTo(1);
		assertThat(response.records()).hasSize(1);
		assertThat(response.records().getFirst().status()).isEqualTo(AttendanceStatus.LEAVE);
		assertThat(response.records().getFirst().approvedLeave()).isTrue();
	}

	@Test
	void saveDailyRejectsChangingApprovedLeaveToAttendanceStatus() {
		LocalDate date = LocalDate.of(2026, 8, 3);
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(leaveRequestRepository.existsApprovedForStaffAndDate(staff.getId(), date)).thenReturn(true);

		StaffAttendanceDailyRequest request = new StaffAttendanceDailyRequest(
				date,
				department.getId(),
				null,
				List.of(new StaffAttendanceEntryRequest(staff.getId(), AttendanceStatus.PRESENT, null)));

		assertThatThrownBy(() -> attendanceService.saveDaily(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
		verify(attendanceRepository, never()).save(any(StaffAttendance.class));
	}

	@Test
	void saveDailyUpsertsAllowedStaffAttendance() {
		LocalDate date = LocalDate.of(2026, 8, 4);
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(leaveRequestRepository.existsApprovedForStaffAndDate(staff.getId(), date)).thenReturn(false);
		when(attendanceRepository.findByStaffIdAndAttendanceDateAndDeletedFalse(staff.getId(), date))
				.thenReturn(Optional.empty());
		when(staffRepository.findRoster(date, department.getId(), null)).thenReturn(List.of(staff));
		when(attendanceRepository.findByStaffIdInAndAttendanceDateAndDeletedFalse(List.of(staff.getId()), date))
				.thenReturn(List.of(new StaffAttendance(staff, date, AttendanceStatus.LATE, "Late bus")));
		when(leaveRequestRepository.findApprovedCoveringDate(List.of(staff.getId()), date)).thenReturn(List.of());

		var response = attendanceService.saveDaily(new StaffAttendanceDailyRequest(
				date,
				department.getId(),
				null,
				List.of(new StaffAttendanceEntryRequest(staff.getId(), AttendanceStatus.LATE, "Late bus"))));

		assertThat(response.records().getFirst().status()).isEqualTo(AttendanceStatus.LATE);
		verify(attendanceRepository).save(any(StaffAttendance.class));
	}

	@Test
	void summaryCountsStatusesAndUsesCanonicalPercentageCalculator() {
		LocalDate fromDate = LocalDate.of(2026, 8, 1);
		LocalDate toDate = LocalDate.of(2026, 8, 31);
		List<StaffAttendance> records = List.of(
				new StaffAttendance(staff, LocalDate.of(2026, 8, 3), AttendanceStatus.PRESENT, null),
				new StaffAttendance(staff, LocalDate.of(2026, 8, 4), AttendanceStatus.LATE, null),
				new StaffAttendance(staff, LocalDate.of(2026, 8, 5), AttendanceStatus.HALF_DAY, null),
				new StaffAttendance(staff, LocalDate.of(2026, 8, 6), AttendanceStatus.ABSENT, null),
				new StaffAttendance(staff, LocalDate.of(2026, 8, 7), AttendanceStatus.LEAVE, null));
		when(attendanceRepository.findSummaryRecords(fromDate, toDate, department.getId(), designation.getId()))
				.thenReturn(records);
		when(staffRepository.findRoster(toDate, department.getId(), designation.getId())).thenReturn(List.of(staff));

		var response = attendanceService.summary(fromDate, toDate, department.getId(), designation.getId());

		assertThat(response.eligibleStaffCount()).isEqualTo(1);
		assertThat(response.totalRecords()).isEqualTo(5);
		assertThat(response.presentCount()).isEqualTo(1);
		assertThat(response.lateCount()).isEqualTo(1);
		assertThat(response.halfDayCount()).isEqualTo(1);
		assertThat(response.absentCount()).isEqualTo(1);
		assertThat(response.leaveCount()).isEqualTo(1);
		assertThat(response.attendancePercentage()).isEqualByComparingTo("50.00");
	}

	@Test
	void monthlySummaryUsesCalendarMonthBoundaries() {
		LocalDate fromDate = LocalDate.of(2026, 2, 1);
		LocalDate toDate = LocalDate.of(2026, 2, 28);
		when(attendanceRepository.findSummaryRecords(fromDate, toDate, null, null)).thenReturn(List.of());
		when(staffRepository.findRoster(toDate, null, null)).thenReturn(List.of(staff));

		var response = attendanceService.monthly(2026, 2, null, null);

		assertThat(response.fromDate()).isEqualTo(fromDate);
		assertThat(response.toDate()).isEqualTo(toDate);
		assertThat(response.eligibleStaffCount()).isEqualTo(1);
		assertThat(response.attendancePercentage()).isEqualByComparingTo("0.00");
	}

	@Test
	void historyUsesDateRangeAndMarksApprovedLeaveRows() {
		LocalDate fromDate = LocalDate.of(2026, 8, 1);
		LocalDate toDate = LocalDate.of(2026, 8, 31);
		LocalDate leaveDate = LocalDate.of(2026, 8, 12);
		StaffAttendance leaveAttendance = new StaffAttendance(staff, leaveDate, AttendanceStatus.LEAVE, "Approved leave");
		when(staffService.loadStaff(staff.getId())).thenReturn(staff);
		when(attendanceRepository.findByStaffIdAndAttendanceDateBetweenAndDeletedFalseOrderByAttendanceDateDesc(
				staff.getId(),
				fromDate,
				toDate))
				.thenReturn(List.of(leaveAttendance));
		when(leaveRequestRepository.existsApprovedForStaffAndDate(staff.getId(), leaveDate)).thenReturn(true);

		var response = attendanceService.history(staff.getId(), fromDate, toDate);

		assertThat(response).hasSize(1);
		assertThat(response.getFirst().status()).isEqualTo(AttendanceStatus.LEAVE);
		assertThat(response.getFirst().approvedLeave()).isTrue();
	}

	private StaffLeaveRequest leave(LocalDate startDate, LocalDate endDate) {
		LeaveType leaveType = new LeaveType("Casual Leave", "CL", true, true);
		setId(leaveType);
		StaffLeaveRequest leave = new StaffLeaveRequest(staff, leaveType, startDate, endDate, "Family", "self");
		leave.approve("principal", null);
		setId(leave);
		return leave;
	}

	private Staff staff(String employeeCode) {
		Staff value = new Staff(
				employeeCode,
				"Ravi",
				null,
				"Menon",
				Gender.MALE,
				LocalDate.of(1988, 4, 20),
				"ravi.menon@school.test",
				"9890000020",
				null,
				null,
				department,
				designation,
				LocalDate.of(2024, 6, 1),
				StaffType.NON_TEACHING,
				EmploymentStatus.ACTIVE);
		setId(value);
		return value;
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
