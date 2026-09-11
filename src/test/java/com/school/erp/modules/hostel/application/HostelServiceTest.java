package com.school.erp.modules.hostel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.application.StudentFeeAutoAssignmentService;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeScope;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.hostel.api.dto.ChangeRoomRequest;
import com.school.erp.modules.hostel.api.dto.VacateHostelRequest;
import com.school.erp.modules.hostel.domain.Hostel;
import com.school.erp.modules.hostel.domain.HostelAllocation;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.hostel.domain.HostelRoom;
import com.school.erp.modules.hostel.infrastructure.HostelAllocationRepository;
import com.school.erp.modules.hostel.infrastructure.HostelBedRepository;
import com.school.erp.modules.hostel.infrastructure.HostelFeeStructureRepository;
import com.school.erp.modules.hostel.infrastructure.HostelRepository;
import com.school.erp.modules.hostel.infrastructure.HostelRoomRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HostelServiceTest {

	@Mock
	private HostelRepository hostelRepository;

	@Mock
	private HostelRoomRepository roomRepository;

	@Mock
	private HostelBedRepository bedRepository;

	@Mock
	private HostelAllocationRepository allocationRepository;

	@Mock
	private HostelFeeStructureRepository hostelFeeStructureRepository;

	@Mock
	private FeeCategoryRepository feeCategoryRepository;

	@Mock
	private FeeStructureRepository feeStructureRepository;

	@Mock
	private StudentFeeAssignmentRepository studentFeeAssignmentRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private FeeService feeService;

	@Mock
	private StudentFeeAutoAssignmentService feeAutoAssignmentService;

	@Mock
	private AuditLogService auditLogService;

	private HostelService service;

	@BeforeEach
	void setUp() {
		service = new HostelService(
				hostelRepository,
				roomRepository,
				bedRepository,
				allocationRepository,
				hostelFeeStructureRepository,
				feeCategoryRepository,
				feeStructureRepository,
				studentFeeAssignmentRepository,
				studentRepository,
				academicHierarchyService,
				feeService,
				feeAutoAssignmentService,
				new HostelMapper(),
				auditLogService);
	}

	@Test
	void changeRoomFlushesClosedAllocationBeforeSavingReplacement() {
		AcademicYear academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		Hostel hostel = new Hostel("MAIN", "Main Hostel", null);
		HostelRoom room = new HostelRoom(hostel, "101", "STANDARD", 1, false);
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 5, 12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		setId(academicYear);
		setId(hostel);
		setId(room);
		setId(student);
		HostelAllocation current = new HostelAllocation(
				student,
				academicYear,
				hostel,
				room,
				null,
				LocalDate.of(2026, 4, 1));
		setId(current);

		when(allocationRepository.findDetailedByIdAndDeletedFalse(current.getId())).thenReturn(Optional.of(current));
		when(roomRepository.findDetailedByIdAndDeletedFalse(room.getId())).thenReturn(Optional.of(room));
		when(roomRepository.lockDetailedByIdAndDeletedFalse(room.getId())).thenReturn(Optional.of(room));
		when(allocationRepository.countByAcademicYearIdAndRoomIdAndStatusAndDeletedFalse(
				academicYear.getId(),
				room.getId(),
				HostelAllocationStatus.ACTIVE))
				.thenReturn(0L);
		when(allocationRepository.save(any(HostelAllocation.class))).thenAnswer(invocation -> {
			HostelAllocation replacement = invocation.getArgument(0);
			setId(replacement);
			return replacement;
		});
		when(studentFeeAssignmentRepository.existsActiveHostelAssignmentForAllocation(
				any(),
				any(),
				any(),
				any(),
				anyString(),
				any(FeeScope.class),
				any(FeeAssignmentStatus.class)))
				.thenReturn(false);

		service.changeRoom(
				current.getId(),
				new ChangeRoomRequest(room.getId(), null, null, LocalDate.of(2026, 9, 1), false));

		assertThat(current.getStatus()).isEqualTo(HostelAllocationStatus.TRANSFERRED);
		verify(feeAutoAssignmentService).reconcileHostelFees(
				student.getId(),
				academicYear.getId(),
				LocalDate.of(2026, 9, 1),
				false);
		InOrder order = inOrder(allocationRepository, roomRepository);
		order.verify(allocationRepository).flush();
		order.verify(roomRepository).lockDetailedByIdAndDeletedFalse(room.getId());
		order.verify(allocationRepository).save(any(HostelAllocation.class));
	}

	@Test
	void vacateReconcilesHostelFeesAfterClosingAllocation() {
		AcademicYear academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		Hostel hostel = new Hostel("MAIN", "Main Hostel", null);
		HostelRoom room = new HostelRoom(hostel, "101", "STANDARD", 1, false);
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 5, 12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		setId(academicYear);
		setId(hostel);
		setId(room);
		setId(student);
		HostelAllocation allocation = new HostelAllocation(
				student,
				academicYear,
				hostel,
				room,
				null,
				LocalDate.of(2026, 4, 1));
		setId(allocation);
		when(allocationRepository.findDetailedByIdAndDeletedFalse(allocation.getId()))
				.thenReturn(Optional.of(allocation));

		service.vacate(allocation.getId(), new VacateHostelRequest(LocalDate.of(2026, 9, 1)));

		assertThat(allocation.getStatus()).isEqualTo(HostelAllocationStatus.VACATED);
		verify(feeAutoAssignmentService).reconcileHostelFees(
				student.getId(),
				academicYear.getId(),
				LocalDate.of(2026, 9, 1),
				false);
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
