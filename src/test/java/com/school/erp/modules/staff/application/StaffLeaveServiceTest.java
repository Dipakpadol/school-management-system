package com.school.erp.modules.staff.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.staff.api.dto.LeaveTypeRequest;
import com.school.erp.modules.staff.api.dto.StaffLeaveCreateRequest;
import com.school.erp.modules.staff.api.dto.StaffLeaveReviewRequest;
import com.school.erp.modules.staff.domain.Department;
import com.school.erp.modules.staff.domain.Designation;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.LeaveType;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.LeaveTypeRepository;
import com.school.erp.modules.staff.infrastructure.StaffLeaveRequestRepository;
import com.school.erp.modules.students.domain.Gender;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffLeaveServiceTest {

	@Mock
	private LeaveTypeRepository leaveTypeRepository;

	@Mock
	private StaffLeaveRequestRepository leaveRequestRepository;

	@Mock
	private StaffService staffService;

	@Mock
	private StaffAttendanceService staffAttendanceService;

	@Mock
	private AuditLogService auditLogService;

	private StaffLeaveService leaveService;
	private Staff staff;
	private LeaveType leaveType;

	@BeforeEach
	void setUp() {
		leaveService = new StaffLeaveService(
				leaveTypeRepository,
				leaveRequestRepository,
				staffService,
				staffAttendanceService,
				new StaffMapper(),
				auditLogService);
		Department department = new Department("Teaching", null, true);
		Designation designation = new Designation("Teacher", department, null, true);
		setId(department);
		setId(designation);
		staff = new Staff(
				"EMP-020",
				"Meera",
				null,
				"Iyer",
				Gender.FEMALE,
				LocalDate.of(1987, 1, 10),
				"meera.iyer@school.test",
				"9890000030",
				null,
				null,
				department,
				designation,
				LocalDate.of(2021, 6, 1),
				StaffType.TEACHING,
				EmploymentStatus.ACTIVE);
		setId(staff);
		leaveType = new LeaveType("Sick Leave", "Medical leave", true, true);
		setId(leaveType);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createLeaveTypeDefaultsActiveAndUnpaidWhenFlagsAreOmitted() {
		when(leaveTypeRepository.findByNameIgnoreCaseAndDeletedFalse("Earned Leave")).thenReturn(Optional.empty());
		when(leaveTypeRepository.save(any(LeaveType.class))).thenAnswer(invocation -> {
			LeaveType persisted = invocation.getArgument(0);
			setId(persisted);
			return persisted;
		});

		var response = leaveService.createLeaveType(new LeaveTypeRequest("Earned Leave", "Annual quota", null, null));

		assertThat(response.name()).isEqualTo("Earned Leave");
		assertThat(response.paid()).isFalse();
		assertThat(response.active()).isTrue();
	}

	@Test
	void requestLeaveRejectsOverlappingPendingOrApprovedLeave() {
		LocalDate start = LocalDate.of(2026, 9, 10);
		LocalDate end = LocalDate.of(2026, 9, 12);
		when(staffService.loadActiveStaff(staff.getId())).thenReturn(staff);
		when(leaveTypeRepository.findByIdAndDeletedFalse(leaveType.getId())).thenReturn(Optional.of(leaveType));
		when(leaveRequestRepository.existsOverlap(eq(staff.getId()), eq(start), eq(end), any(), eq(null))).thenReturn(true);

		assertThatThrownBy(() -> leaveService.requestLeave(new StaffLeaveCreateRequest(
				staff.getId(),
				leaveType.getId(),
				start,
				end,
				"Medical")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void approvePendingLeaveMarksAttendanceAsLeave() {
		UUID leaveRequestId = UUID.randomUUID();
		StaffLeaveRequest leaveRequest = new StaffLeaveRequest(
				staff,
				leaveType,
				LocalDate.of(2026, 9, 10),
				LocalDate.of(2026, 9, 12),
				"Medical",
				"teacher");
		setId(leaveRequest, leaveRequestId);
		when(leaveRequestRepository.findByIdAndDeletedFalse(leaveRequestId)).thenReturn(Optional.of(leaveRequest));
		when(leaveRequestRepository.existsOverlap(
				staff.getId(),
				leaveRequest.getStartDate(),
				leaveRequest.getEndDate(),
				java.util.List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
				leaveRequestId))
				.thenReturn(false);

		var response = leaveService.approve(leaveRequestId, new StaffLeaveReviewRequest("Approved"));

		assertThat(response.status()).isEqualTo(LeaveStatus.APPROVED);
		assertThat(response.reviewComment()).isEqualTo("Approved");
		verify(staffAttendanceService).applyApprovedLeave(leaveRequest);
	}

	@Test
	void rejectRequiresLeaveApprovalAuthorityWhenAuthenticated() {
		authenticate("LEAVE_READ");

		assertThatThrownBy(() -> leaveService.reject(UUID.randomUUID(), new StaffLeaveReviewRequest("No balance")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	void rejectPendingLeaveStoresReviewDecision() {
		authenticate("LEAVE_APPROVE");
		UUID leaveRequestId = UUID.randomUUID();
		StaffLeaveRequest leaveRequest = leaveRequest(leaveRequestId);
		when(leaveRequestRepository.findByIdAndDeletedFalse(leaveRequestId)).thenReturn(Optional.of(leaveRequest));

		var response = leaveService.reject(leaveRequestId, new StaffLeaveReviewRequest("Insufficient balance"));

		assertThat(response.status()).isEqualTo(LeaveStatus.REJECTED);
		assertThat(response.reviewedBy()).isEqualTo("principal");
		assertThat(response.reviewedAt()).isNotNull();
		assertThat(response.reviewComment()).isEqualTo("Insufficient balance");
	}

	@Test
	void cancelPendingLeaveStoresCancellationDecision() {
		UUID leaveRequestId = UUID.randomUUID();
		StaffLeaveRequest leaveRequest = leaveRequest(leaveRequestId);
		when(leaveRequestRepository.findByIdAndDeletedFalse(leaveRequestId)).thenReturn(Optional.of(leaveRequest));

		var response = leaveService.cancel(leaveRequestId, new StaffLeaveReviewRequest("No longer needed"));

		assertThat(response.status()).isEqualTo(LeaveStatus.CANCELLED);
		assertThat(response.reviewComment()).isEqualTo("No longer needed");
	}

	private StaffLeaveRequest leaveRequest(UUID id) {
		StaffLeaveRequest leaveRequest = new StaffLeaveRequest(
				staff,
				leaveType,
				LocalDate.of(2026, 9, 10),
				LocalDate.of(2026, 9, 12),
				"Medical",
				"teacher");
		setId(leaveRequest, id);
		return leaveRequest;
	}

	private void authenticate(String... authorities) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"principal",
				null,
				java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
	}

	private void setId(Object entity) {
		setId(entity, UUID.randomUUID());
	}

	private void setId(Object entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
	}
}
