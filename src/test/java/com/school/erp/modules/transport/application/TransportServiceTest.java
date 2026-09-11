package com.school.erp.modules.transport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.application.StudentFeeAutoAssignmentService;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.transport.api.dto.TransportAssignmentRequest;
import com.school.erp.modules.transport.api.dto.TransportRemoveRequest;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportPickupPoint;
import com.school.erp.modules.transport.domain.TransportRoute;
import com.school.erp.modules.transport.domain.TransportStatus;
import com.school.erp.modules.transport.domain.TransportVehicle;
import com.school.erp.modules.transport.infrastructure.StudentTransportAssignmentRepository;
import com.school.erp.modules.transport.infrastructure.TransportDriverRepository;
import com.school.erp.modules.transport.infrastructure.TransportFeeStructureRepository;
import com.school.erp.modules.transport.infrastructure.TransportPickupPointRepository;
import com.school.erp.modules.transport.infrastructure.TransportRouteRepository;
import com.school.erp.modules.transport.infrastructure.TransportVehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransportServiceTest {

	@Mock
	private TransportDriverRepository driverRepository;

	@Mock
	private TransportVehicleRepository vehicleRepository;

	@Mock
	private TransportRouteRepository routeRepository;

	@Mock
	private TransportPickupPointRepository pickupPointRepository;

	@Mock
	private StudentTransportAssignmentRepository assignmentRepository;

	@Mock
	private TransportFeeStructureRepository transportFeeStructureRepository;

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

	private TransportService service;
	private AcademicYear academicYear;
	private TransportVehicle vehicle;
	private TransportRoute route;
	private TransportPickupPoint pickupPoint;
	private Student student;

	@BeforeEach
	void setUp() {
		service = new TransportService(
				driverRepository,
				vehicleRepository,
				routeRepository,
				pickupPointRepository,
				assignmentRepository,
				transportFeeStructureRepository,
				feeCategoryRepository,
				feeStructureRepository,
				studentFeeAssignmentRepository,
				studentRepository,
				academicHierarchyService,
				feeService,
				feeAutoAssignmentService,
				new TransportMapper(),
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		vehicle = new TransportVehicle(academicYear, "BUS-1", "Bus 1", "BUS", 1, null, TransportStatus.ACTIVE);
		route = new TransportRoute(academicYear, "North Route", "NORTH", "A", "B", vehicle, TransportStatus.ACTIVE);
		pickupPoint = new TransportPickupPoint(route, "Main Stop", null, null, null, 1, TransportStatus.ACTIVE);
		student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 5, 12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		setId(academicYear);
		setId(vehicle);
		setId(route);
		setId(pickupPoint);
		setId(student);
	}

	@Test
	void changeStudentAssignmentFlushesClosedAssignmentBeforeSavingReplacement() {
		StudentTransportAssignment current = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(current.getId())).thenReturn(Optional.of(current));
		when(routeRepository.findByIdAndDeletedFalse(route.getId())).thenReturn(Optional.of(route));
		when(pickupPointRepository.findByIdAndDeletedFalse(pickupPoint.getId())).thenReturn(Optional.of(pickupPoint));
		when(vehicleRepository.lockByIdAndDeletedFalse(vehicle.getId())).thenReturn(Optional.of(vehicle));
		when(assignmentRepository.countByVehicleIdAndAcademicYearIdAndStatusAndDeletedFalse(
				vehicle.getId(),
				academicYear.getId(),
				TransportStatus.ASSIGNED))
				.thenReturn(0L);
		when(assignmentRepository.save(any(StudentTransportAssignment.class))).thenAnswer(invocation -> {
			StudentTransportAssignment replacement = invocation.getArgument(0);
			setId(replacement);
			return replacement;
		});

		service.changeStudentAssignment(student.getId(), current.getId(), request());

		assertThat(current.getStatus()).isEqualTo(TransportStatus.TRANSFERRED);
		verify(feeAutoAssignmentService).reconcileTransportFees(
				student.getId(),
				academicYear.getId(),
				LocalDate.of(2026, 9, 1),
				false);
		InOrder order = inOrder(assignmentRepository, vehicleRepository);
		order.verify(assignmentRepository).flush();
		order.verify(vehicleRepository).lockByIdAndDeletedFalse(vehicle.getId());
		order.verify(assignmentRepository).save(any(StudentTransportAssignment.class));
	}

	@Test
	void removeStudentAssignmentReconcilesTransportFeesAfterRemoval() {
		StudentTransportAssignment current = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(current.getId())).thenReturn(Optional.of(current));

		service.removeStudentAssignment(
				student.getId(),
				current.getId(),
				new TransportRemoveRequest(LocalDate.of(2026, 9, 1)));

		assertThat(current.getStatus()).isEqualTo(TransportStatus.REMOVED);
		verify(feeAutoAssignmentService).reconcileTransportFees(
				student.getId(),
				academicYear.getId(),
				LocalDate.of(2026, 9, 1),
				false);
	}

	@Test
	void changeStudentAssignmentRejectsInactiveRoute() {
		StudentTransportAssignment current = assignment();
		TransportRoute inactiveRoute = new TransportRoute(
				academicYear,
				"Inactive Route",
				"INACTIVE",
				"A",
				"B",
				vehicle,
				TransportStatus.INACTIVE);
		TransportPickupPoint inactiveRoutePoint = new TransportPickupPoint(
				inactiveRoute,
				"Inactive Stop",
				null,
				null,
				null,
				1,
				TransportStatus.ACTIVE);
		setId(inactiveRoute);
		setId(inactiveRoutePoint);
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(current.getId())).thenReturn(Optional.of(current));
		when(routeRepository.findByIdAndDeletedFalse(inactiveRoute.getId())).thenReturn(Optional.of(inactiveRoute));
		when(pickupPointRepository.findByIdAndDeletedFalse(inactiveRoutePoint.getId())).thenReturn(Optional.of(inactiveRoutePoint));

		assertThatThrownBy(() -> service.changeStudentAssignment(
				student.getId(),
				current.getId(),
				new TransportAssignmentRequest(
						true,
						null,
						null,
						null,
						inactiveRoute.getId(),
						null,
						inactiveRoutePoint.getId(),
						null,
						LocalDate.of(2026, 9, 1),
						false)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Route is inactive.")
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
		assertThat(current.getStatus()).isEqualTo(TransportStatus.ASSIGNED);
		verify(assignmentRepository, never()).flush();
		verify(assignmentRepository, never()).save(any(StudentTransportAssignment.class));
	}

	private StudentTransportAssignment assignment() {
		StudentTransportAssignment assignment = new StudentTransportAssignment(
				student,
				academicYear,
				vehicle,
				route,
				pickupPoint,
				LocalDate.of(2026, 4, 1));
		setId(assignment);
		return assignment;
	}

	private TransportAssignmentRequest request() {
		return new TransportAssignmentRequest(
				true,
				null,
				null,
				null,
				route.getId(),
				null,
				pickupPoint.getId(),
				null,
				LocalDate.of(2026, 9, 1),
				false);
	}

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
