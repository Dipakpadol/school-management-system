package com.school.erp.modules.staff.application;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.attendance.application.AttendanceSummaryCalculator;
import com.school.erp.modules.attendance.domain.AttendanceStatus;
import com.school.erp.modules.staff.api.dto.StaffAttendanceDailyRequest;
import com.school.erp.modules.staff.api.dto.StaffAttendanceDailyResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceRecordResponse;
import com.school.erp.modules.staff.api.dto.StaffAttendanceSummaryResponse;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffAttendance;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;
import com.school.erp.modules.staff.infrastructure.StaffAttendanceRepository;
import com.school.erp.modules.staff.infrastructure.StaffLeaveRequestRepository;
import com.school.erp.modules.staff.infrastructure.StaffRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffAttendanceService {

	private static final String MODULE_NAME = "STAFF_ATTENDANCE";

	private final StaffRepository staffRepository;
	private final StaffAttendanceRepository attendanceRepository;
	private final StaffLeaveRequestRepository leaveRequestRepository;
	private final StaffService staffService;
	private final StaffMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public StaffAttendanceDailyResponse daily(LocalDate date, UUID departmentId, UUID designationId) {
		LocalDate attendanceDate = requiredDate(date);
		List<Staff> roster = staffRepository.findRoster(attendanceDate, departmentId, designationId);
		if (roster.isEmpty()) {
			return new StaffAttendanceDailyResponse(attendanceDate, 0, List.of());
		}
		List<UUID> staffIds = roster.stream().map(Staff::getId).toList();
		Map<UUID, StaffAttendance> existing = attendanceRepository
				.findByStaffIdInAndAttendanceDateAndDeletedFalse(staffIds, attendanceDate)
				.stream()
				.collect(Collectors.toMap(record -> record.getStaff().getId(), Function.identity(), (first, second) -> first));
		Map<UUID, StaffLeaveRequest> approvedLeaves = leaveRequestRepository
				.findApprovedCoveringDate(staffIds, attendanceDate)
				.stream()
				.collect(Collectors.toMap(request -> request.getStaff().getId(), Function.identity(), (first, second) -> first));
		List<StaffAttendanceRecordResponse> records = roster.stream()
				.map(staff -> {
					StaffAttendance attendance = existing.get(staff.getId());
					boolean approvedLeave = approvedLeaves.containsKey(staff.getId());
					if (attendance != null) {
						return mapper.toAttendanceResponse(attendance, approvedLeave);
					}
					AttendanceStatus status = approvedLeave ? AttendanceStatus.LEAVE : null;
					return mapper.toAttendanceResponse(staff, attendanceDate, status, null, approvedLeave);
				})
				.toList();
		return new StaffAttendanceDailyResponse(attendanceDate, roster.size(), records);
	}

	@Transactional
	public StaffAttendanceDailyResponse saveDaily(StaffAttendanceDailyRequest request) {
		LocalDate attendanceDate = requiredDate(request.date());
		List<com.school.erp.modules.staff.api.dto.StaffAttendanceEntryRequest> entries =
				request.records() == null ? List.of() : request.records();
		for (var entry : entries) {
			Staff staff = staffService.loadActiveStaff(entry.staffId());
			validateRosterMembership(staff, attendanceDate, request.departmentId(), request.designationId());
			boolean approvedLeave = leaveRequestRepository.existsApprovedForStaffAndDate(staff.getId(), attendanceDate);
			if (approvedLeave && entry.status() != AttendanceStatus.LEAVE) {
				throw new BusinessException(
						ErrorCode.BUSINESS_RULE_VIOLATION,
						"Approved leave exists for " + staff.getEmployeeCode() + "; attendance must be LEAVE.");
			}
			StaffAttendance attendance = attendanceRepository
					.findByStaffIdAndAttendanceDateAndDeletedFalse(staff.getId(), attendanceDate)
					.orElseGet(() -> new StaffAttendance(staff, attendanceDate, entry.status(), entry.remarks()));
			attendance.update(entry.status(), entry.remarks());
			attendanceRepository.save(attendance);
		}
		audit("StaffAttendance", null, "DAILY_SAVE", null, Map.of(
				"date", attendanceDate,
				"recordCount", entries.size()));
		return daily(attendanceDate, request.departmentId(), request.designationId());
	}

	@Transactional(readOnly = true)
	public StaffAttendanceSummaryResponse summary(
			LocalDate fromDate,
			LocalDate toDate,
			UUID departmentId,
			UUID designationId) {
		validateDateRange(fromDate, toDate);
		List<StaffAttendance> records = attendanceRepository.findSummaryRecords(fromDate, toDate, departmentId, designationId);
		long eligibleCount = staffRepository.findRoster(toDate, departmentId, designationId).size();
		return new StaffAttendanceSummaryResponse(
				departmentId,
				null,
				designationId,
				null,
				fromDate,
				toDate,
				eligibleCount,
				records.size(),
				count(records, AttendanceStatus.PRESENT),
				count(records, AttendanceStatus.ABSENT),
				count(records, AttendanceStatus.LATE),
				count(records, AttendanceStatus.HALF_DAY),
				count(records, AttendanceStatus.LEAVE),
				AttendanceSummaryCalculator.percentage(records, StaffAttendance::getStatus));
	}

	@Transactional(readOnly = true)
	public StaffAttendanceSummaryResponse monthly(int year, int month, UUID departmentId, UUID designationId) {
		YearMonth yearMonth = YearMonth.of(year, month);
		return summary(yearMonth.atDay(1), yearMonth.atEndOfMonth(), departmentId, designationId);
	}

	@Transactional(readOnly = true)
	public List<StaffAttendanceRecordResponse> history(UUID staffId, LocalDate fromDate, LocalDate toDate) {
		Staff staff = staffService.loadStaff(staffId);
		LocalDate from = fromDate == null ? staff.getJoiningDate() : fromDate;
		LocalDate to = toDate == null ? LocalDate.now() : toDate;
		validateDateRange(from, to);
		return attendanceRepository
				.findByStaffIdAndAttendanceDateBetweenAndDeletedFalseOrderByAttendanceDateDesc(staffId, from, to)
				.stream()
				.map(record -> mapper.toAttendanceResponse(
						record,
						leaveRequestRepository.existsApprovedForStaffAndDate(staffId, record.getAttendanceDate())))
				.toList();
	}

	@Transactional
	public void applyApprovedLeave(StaffLeaveRequest leaveRequest) {
		LocalDate date = leaveRequest.getStartDate();
		while (!date.isAfter(leaveRequest.getEndDate())) {
			LocalDate attendanceDate = date;
			StaffAttendance attendance = attendanceRepository
					.findByStaffIdAndAttendanceDateAndDeletedFalse(leaveRequest.getStaff().getId(), attendanceDate)
					.orElseGet(() -> new StaffAttendance(
							leaveRequest.getStaff(),
							attendanceDate,
							AttendanceStatus.LEAVE,
							"Approved leave: " + leaveRequest.getLeaveType().getName()));
			attendance.update(AttendanceStatus.LEAVE, "Approved leave: " + leaveRequest.getLeaveType().getName());
			attendanceRepository.save(attendance);
			date = date.plusDays(1);
		}
	}

	public List<Map<String, Object>> reportRows(LocalDate fromDate, LocalDate toDate, UUID departmentId, UUID designationId) {
		validateDateRange(fromDate, toDate);
		return attendanceRepository.findSummaryRecords(fromDate, toDate, departmentId, designationId).stream()
				.map(this::attendanceRow)
				.toList();
	}

	private Map<String, Object> attendanceRow(StaffAttendance attendance) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("Date", attendance.getAttendanceDate());
		row.put("Employee Code", attendance.getStaff().getEmployeeCode());
		row.put("Staff Name", attendance.getStaff().getDisplayName());
		row.put("Department", attendance.getStaff().getDepartment() == null ? null : attendance.getStaff().getDepartment().getName());
		row.put("Designation", attendance.getStaff().getDesignation() == null ? null : attendance.getStaff().getDesignation().getName());
		row.put("Status", attendance.getStatus());
		row.put("Remarks", attendance.getRemarks());
		return row;
	}

	private void validateRosterMembership(Staff staff, LocalDate attendanceDate, UUID departmentId, UUID designationId) {
		if (!staff.isEligibleOn(attendanceDate)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Staff is not active for the attendance date.");
		}
		if (departmentId != null && (staff.getDepartment() == null || !staff.getDepartment().getId().equals(departmentId))) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Staff does not belong to the selected department.");
		}
		if (designationId != null && (staff.getDesignation() == null || !staff.getDesignation().getId().equals(designationId))) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Staff does not belong to the selected designation.");
		}
	}

	private LocalDate requiredDate(LocalDate date) {
		if (date == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Attendance date is required.");
		}
		return date;
	}

	private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
		if (fromDate == null || toDate == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "From date and to date are required.");
		}
		if (toDate.isBefore(fromDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "To date cannot be before from date.");
		}
	}

	private long count(List<StaffAttendance> records, AttendanceStatus status) {
		return records.stream().filter(record -> record.getStatus() == status).count();
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

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}
}
