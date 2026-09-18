package com.school.erp.modules.staff.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.staff.api.dto.LeaveTypeRequest;
import com.school.erp.modules.staff.api.dto.LeaveTypeResponse;
import com.school.erp.modules.staff.api.dto.StaffLeaveCreateRequest;
import com.school.erp.modules.staff.api.dto.StaffLeaveResponse;
import com.school.erp.modules.staff.api.dto.StaffLeaveReviewRequest;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.LeaveType;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;
import com.school.erp.modules.staff.infrastructure.LeaveTypeRepository;
import com.school.erp.modules.staff.infrastructure.StaffLeaveRequestRepository;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffLeaveService {

	private static final String MODULE_NAME = "LEAVE";
	private static final Collection<LeaveStatus> BLOCKING_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);

	private final LeaveTypeRepository leaveTypeRepository;
	private final StaffLeaveRequestRepository leaveRequestRepository;
	private final StaffService staffService;
	private final StaffAttendanceService staffAttendanceService;
	private final StaffMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<LeaveTypeResponse> leaveTypes() {
		return leaveTypeRepository.findAllByDeletedFalseOrderByNameAsc().stream()
				.map(mapper::toLeaveTypeResponse)
				.toList();
	}

	@Transactional
	public LeaveTypeResponse createLeaveType(LeaveTypeRequest request) {
		validateLeaveTypeName(request.name(), null, active(request.active()));
		LeaveType leaveType = leaveTypeRepository.save(new LeaveType(
				request.name(),
				request.description(),
				Boolean.TRUE.equals(request.paid()),
				active(request.active())));
		LeaveTypeResponse response = mapper.toLeaveTypeResponse(leaveType);
		audit("LeaveType", leaveType.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public LeaveTypeResponse updateLeaveType(UUID leaveTypeId, LeaveTypeRequest request) {
		LeaveType leaveType = loadLeaveType(leaveTypeId);
		LeaveTypeResponse oldValue = mapper.toLeaveTypeResponse(leaveType);
		boolean active = active(request.active());
		validateLeaveTypeName(request.name(), leaveTypeId, active);
		leaveType.update(request.name(), request.description(), Boolean.TRUE.equals(request.paid()), active);
		LeaveTypeResponse response = mapper.toLeaveTypeResponse(leaveType);
		audit("LeaveType", leaveTypeId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<StaffLeaveResponse> leaves(
			UUID staffId,
			LeaveStatus status,
			LocalDate fromDate,
			LocalDate toDate,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				leaveRequestRepository.search(staffId, status, fromDate, toDate, pageRequest.toPageable("startDate")),
				mapper::toLeaveResponse);
	}

	@Transactional
	public StaffLeaveResponse requestLeave(StaffLeaveCreateRequest request) {
		validateDateRange(request.startDate(), request.endDate());
		Staff staff = staffService.loadActiveStaff(request.staffId());
		LeaveType leaveType = loadActiveLeaveType(request.leaveTypeId());
		validateNoOverlap(staff.getId(), request.startDate(), request.endDate(), null);
		StaffLeaveRequest leaveRequest = leaveRequestRepository.save(new StaffLeaveRequest(
				staff,
				leaveType,
				request.startDate(),
				request.endDate(),
				request.reason(),
				currentActor()));
		StaffLeaveResponse response = mapper.toLeaveResponse(leaveRequest);
		audit("StaffLeaveRequest", leaveRequest.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public StaffLeaveResponse approve(UUID leaveRequestId, StaffLeaveReviewRequest request) {
		requireAuthority("LEAVE_APPROVE");
		StaffLeaveRequest leaveRequest = loadLeaveRequest(leaveRequestId);
		StaffLeaveResponse oldValue = mapper.toLeaveResponse(leaveRequest);
		if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only pending leave can be approved.");
		}
		validateNoOverlap(
				leaveRequest.getStaff().getId(),
				leaveRequest.getStartDate(),
				leaveRequest.getEndDate(),
				leaveRequest.getId());
		leaveRequest.approve(currentActor(), request == null ? null : request.comment());
		staffAttendanceService.applyApprovedLeave(leaveRequest);
		StaffLeaveResponse response = mapper.toLeaveResponse(leaveRequest);
		audit("StaffLeaveRequest", leaveRequestId, "APPROVE", oldValue, response);
		return response;
	}

	@Transactional
	public StaffLeaveResponse reject(UUID leaveRequestId, StaffLeaveReviewRequest request) {
		requireAuthority("LEAVE_APPROVE");
		StaffLeaveRequest leaveRequest = loadLeaveRequest(leaveRequestId);
		StaffLeaveResponse oldValue = mapper.toLeaveResponse(leaveRequest);
		if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only pending leave can be rejected.");
		}
		leaveRequest.reject(currentActor(), request == null ? null : request.comment());
		StaffLeaveResponse response = mapper.toLeaveResponse(leaveRequest);
		audit("StaffLeaveRequest", leaveRequestId, "REJECT", oldValue, response);
		return response;
	}

	@Transactional
	public StaffLeaveResponse cancel(UUID leaveRequestId, StaffLeaveReviewRequest request) {
		StaffLeaveRequest leaveRequest = loadLeaveRequest(leaveRequestId);
		StaffLeaveResponse oldValue = mapper.toLeaveResponse(leaveRequest);
		if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only pending leave can be cancelled.");
		}
		leaveRequest.cancel(currentActor(), request == null ? null : request.comment());
		StaffLeaveResponse response = mapper.toLeaveResponse(leaveRequest);
		audit("StaffLeaveRequest", leaveRequestId, "CANCEL", oldValue, response);
		return response;
	}

	public List<Map<String, Object>> reportRows(UUID staffId, LeaveStatus status, LocalDate fromDate, LocalDate toDate) {
		if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "To date cannot be before from date.");
		}
		return leaveRequestRepository.findReportRows(staffId, status, fromDate, toDate).stream()
				.map(this::leaveRow)
				.toList();
	}

	private Map<String, Object> leaveRow(StaffLeaveRequest request) {
		return new java.util.LinkedHashMap<>(Map.ofEntries(
				Map.entry("Employee Code", request.getStaff().getEmployeeCode()),
				Map.entry("Staff Name", request.getStaff().getDisplayName()),
				Map.entry("Leave Type", request.getLeaveType().getName()),
				Map.entry("Start Date", request.getStartDate()),
				Map.entry("End Date", request.getEndDate()),
				Map.entry("Duration Days", request.getDurationDays()),
				Map.entry("Status", request.getStatus()),
				Map.entry("Requested By", nullSafe(request.getRequestedBy())),
				Map.entry("Reviewed By", nullSafe(request.getReviewedBy()))));
	}

	private LeaveType loadLeaveType(UUID leaveTypeId) {
		return leaveTypeRepository.findByIdAndDeletedFalse(leaveTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave type", leaveTypeId));
	}

	private LeaveType loadActiveLeaveType(UUID leaveTypeId) {
		LeaveType leaveType = loadLeaveType(leaveTypeId);
		if (!leaveType.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Leave type is inactive.");
		}
		return leaveType;
	}

	private StaffLeaveRequest loadLeaveRequest(UUID leaveRequestId) {
		return leaveRequestRepository.findByIdAndDeletedFalse(leaveRequestId)
				.orElseThrow(() -> new ResourceNotFoundException("Leave request", leaveRequestId));
	}

	private void validateLeaveTypeName(String name, UUID excludedId, boolean active) {
		if (!active) {
			return;
		}
		leaveTypeRepository.findByNameIgnoreCaseAndDeletedFalse(name)
				.filter(existing -> existing.isActive())
				.filter(existing -> excludedId == null || !existing.getId().equals(excludedId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Active leave type name already exists.");
				});
	}

	private void validateDateRange(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Leave start date and end date are required.");
		}
		if (endDate.isBefore(startDate)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Leave end date cannot be before start date.");
		}
	}

	private void validateNoOverlap(UUID staffId, LocalDate startDate, LocalDate endDate, UUID excludedId) {
		if (leaveRequestRepository.existsOverlap(staffId, startDate, endDate, BLOCKING_STATUSES, excludedId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Staff already has an overlapping pending or approved leave request.");
		}
	}

	private boolean active(Boolean active) {
		return active == null || active;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void requireAuthority(String authority) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		boolean allowed = authentication.getAuthorities().stream()
				.anyMatch(granted -> granted.getAuthority().equals(authority));
		if (!allowed) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Leave approval permission is required.");
		}
	}

	private String nullSafe(String value) {
		return value == null ? "" : value;
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
}
