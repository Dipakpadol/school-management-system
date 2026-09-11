package com.school.erp.modules.fees.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.academic.domain.SectionEntity;
import com.school.erp.modules.fees.domain.ClassFeeAssignment;
import com.school.erp.modules.fees.domain.ClassFeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeePayment;
import com.school.erp.modules.fees.domain.FeeReceipt;
import com.school.erp.modules.fees.domain.FeeReceiptStatus;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.fees.domain.PaymentMode;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.infrastructure.ClassFeeAssignmentRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.hostel.domain.Hostel;
import com.school.erp.modules.hostel.domain.HostelAllocation;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;
import com.school.erp.modules.hostel.domain.HostelFeeStructure;
import com.school.erp.modules.hostel.domain.HostelRoom;
import com.school.erp.modules.hostel.infrastructure.HostelAllocationRepository;
import com.school.erp.modules.hostel.infrastructure.HostelFeeStructureRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportFeeStructure;
import com.school.erp.modules.transport.domain.TransportPickupPoint;
import com.school.erp.modules.transport.domain.TransportRoute;
import com.school.erp.modules.transport.domain.TransportStatus;
import com.school.erp.modules.transport.domain.TransportVehicle;
import com.school.erp.modules.transport.infrastructure.StudentTransportAssignmentRepository;
import com.school.erp.modules.transport.infrastructure.TransportFeeStructureRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StudentFeeAutoAssignmentServiceTest {

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private ClassFeeAssignmentRepository classFeeAssignmentRepository;

	@Mock
	private HostelFeeStructureRepository hostelFeeStructureRepository;

	@Mock
	private TransportFeeStructureRepository transportFeeStructureRepository;

	@Mock
	private HostelAllocationRepository hostelAllocationRepository;

	@Mock
	private StudentTransportAssignmentRepository transportAssignmentRepository;

	@Mock
	private StudentClassAssignmentRepository studentClassAssignmentRepository;

	@Mock
	private StudentFeeAssignmentRepository assignmentRepository;

	@Mock
	private FeeService feeService;

	@Mock
	private AuditLogService auditLogService;

	private StudentFeeAutoAssignmentService service;
	private AcademicYear academicYear;
	private ClassEntity classEntity;
	private SectionEntity section;

	@BeforeEach
	void setUp() {
		service = new StudentFeeAutoAssignmentService(
				academicHierarchyService,
				classFeeAssignmentRepository,
				hostelFeeStructureRepository,
				transportFeeStructureRepository,
				hostelAllocationRepository,
				transportAssignmentRepository,
				studentClassAssignmentRepository,
				assignmentRepository,
				feeService,
				auditLogService);
		academicYear = new AcademicYear("AY-2026-27", "2026-2027", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));
		classEntity = new ClassEntity(academicYear, "CLASS-6", "Class 6", 6);
		section = new SectionEntity(classEntity, "A", "Division A", 40, 1);
		setId(academicYear);
		setId(classEntity);
		setId(section);
	}

	@Test
	void reconcileStudentFeesUsesCanonicalActiveAcademicEnrollment() {
		UUID studentId = UUID.randomUUID();
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 5, 12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		setId(student, studentId);
		StudentClassAssignment enrollment = student.assignClassSection(
				academicYear,
				classEntity,
				section,
				"23",
				LocalDate.of(2026, 4, 1));
		setId(enrollment);
		FeeStructure structure = new FeeStructure("2026-2027", "Class 6", "A", "Tuition Fee", null);
		structure.updateAcademicMapping(academicYear, classEntity);
		structure.activate();
		setId(structure);
		ClassFeeAssignment classFeeAssignment = new ClassFeeAssignment(
				academicYear,
				classEntity,
				structure,
				LocalDate.of(2026, 4, 1),
				"system");
		setId(classFeeAssignment);

		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(academicHierarchyService.loadClass(classEntity.getId())).thenReturn(classEntity);
		when(studentClassAssignmentRepository.findActiveForStudentAndAcademicYear(studentId, academicYear.getId()))
				.thenReturn(Optional.of(enrollment));
		when(classFeeAssignmentRepository.findActiveByAcademicYearAndClass(
				academicYear.getId(),
				classEntity.getId(),
				ClassFeeAssignmentStatus.ACTIVE))
				.thenReturn(List.of(classFeeAssignment));
		when(feeService.assignActiveClassFeesToStudent(
				studentId,
				academicYear.getId(),
				classEntity.getId(),
				enrollment.getEffectiveFrom()))
				.thenReturn(List.of());
		when(hostelAllocationRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
				studentId,
				academicYear.getId(),
				HostelAllocationStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.HOSTEL))
				.thenReturn(List.of());
		when(transportAssignmentRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
				studentId,
				academicYear.getId(),
				TransportStatus.ASSIGNED))
				.thenReturn(Optional.empty());
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.TRANSPORT))
				.thenReturn(List.of());

		var result = service.reconcileStudentFees(studentId, academicYear.getId());

		assertThat(result.warnings()).isEmpty();
		verify(feeService).assignActiveClassFeesToStudent(
				studentId,
				academicYear.getId(),
				classEntity.getId(),
				enrollment.getEffectiveFrom());
	}

	@Test
	void reconcileHostelFeesCancelsFutureUnpaidObsoleteAssignmentAndAssignsCurrentFee() {
		UUID studentId = UUID.randomUUID();
		Student student = student(studentId);
		Hostel hostel = hostel();
		HostelRoom oldRoom = room(hostel, "101", "STANDARD");
		HostelRoom newRoom = room(hostel, "102", "DELUXE");
		LocalDate transferDate = LocalDate.of(2026, 9, 1);
		HostelAllocation activeAllocation = new HostelAllocation(
				student,
				academicYear,
				hostel,
				newRoom,
				null,
				transferDate);
		setId(activeAllocation);
		FeeCategory category = category("HOSTEL");
		FeeStructure oldStructure = hostelFeeStructure(category, hostel, oldRoom, LocalDate.of(2026, 10, 15), "Old room fee");
		FeeStructure currentStructure = hostelFeeStructure(category, hostel, newRoom, LocalDate.of(2026, 10, 15), "New room fee");
		StudentFeeAssignment obsoleteAssignment = assignment(student, oldStructure);
		HostelFeeStructure currentHostelFee = new HostelFeeStructure(
				academicYear,
				hostel,
				newRoom,
				newRoom.getRoomType(),
				category,
				currentStructure,
				money("1000.00"),
				LocalDate.of(2026, 10, 15),
				false,
				1,
				FeeStructureStatus.ACTIVE);
		setId(currentHostelFee);

		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(hostelAllocationRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
				studentId,
				academicYear.getId(),
				HostelAllocationStatus.ACTIVE))
				.thenReturn(Optional.of(activeAllocation));
		when(hostelFeeStructureRepository.findApplicable(
				academicYear.getId(),
				hostel.getId(),
				newRoom.getId(),
				newRoom.getRoomType(),
				FeeStructureStatus.ACTIVE))
				.thenReturn(List.of(currentHostelFee));
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.HOSTEL))
				.thenReturn(List.of(obsoleteAssignment));
		when(feeService.assignActiveHostelFeesToStudent(
				studentId,
				List.of(currentStructure.getId()),
				transferDate))
				.thenReturn(List.of());

		service.reconcileHostelFees(studentId, academicYear.getId(), transferDate);

		assertThat(obsoleteAssignment.getStatus()).isEqualTo(FeeAssignmentStatus.CANCELLED);
		assertThat(obsoleteAssignment.getBalanceAmount()).isZero();
		verify(feeService).assignActiveHostelFeesToStudent(
				studentId,
				List.of(currentStructure.getId()),
				transferDate);
	}

	@Test
	void reconcileHostelFeesPreservesObsoleteAssignmentWithPaymentHistory() {
		UUID studentId = UUID.randomUUID();
		Student student = student(studentId);
		Hostel hostel = hostel();
		HostelRoom room = room(hostel, "101", "STANDARD");
		LocalDate vacateDate = LocalDate.of(2026, 9, 1);
		FeeStructure structure = hostelFeeStructure(
				category("HOSTEL"),
				hostel,
				room,
				LocalDate.of(2026, 10, 15),
				"Paid hostel fee");
		StudentFeeAssignment assignment = assignment(student, structure);
		FeeReceipt receipt = new FeeReceipt(
				"R-1",
				student,
				assignment,
				money("100.00"),
				"Parent",
				PaymentMode.CASH);
		setId(receipt);
		FeePayment payment = new FeePayment(
				assignment,
				receipt,
				money("100.00"),
				LocalDate.of(2026, 8, 15),
				PaymentMode.CASH,
				null,
				"Parent",
				"accounts",
				null);
		assignment.collectPayment(payment);
		setIds(assignment);

		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(hostelAllocationRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
				studentId,
				academicYear.getId(),
				HostelAllocationStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.HOSTEL))
				.thenReturn(List.of(assignment));

		service.reconcileHostelFees(studentId, academicYear.getId(), vacateDate, false);

		assertThat(assignment.getStatus()).isEqualTo(FeeAssignmentStatus.PARTIALLY_PAID);
		assertThat(assignment.getPayments()).hasSize(1);
		assertThat(receipt.getStatus()).isEqualTo(FeeReceiptStatus.ISSUED);
	}

	@Test
	void reconcileHostelFeesPreservesPastDueUnpaidObsoleteAssignment() {
		UUID studentId = UUID.randomUUID();
		Student student = student(studentId);
		Hostel hostel = hostel();
		HostelRoom room = room(hostel, "101", "STANDARD");
		LocalDate vacateDate = LocalDate.of(2026, 9, 1);
		FeeStructure structure = hostelFeeStructure(
				category("HOSTEL"),
				hostel,
				room,
				LocalDate.of(2026, 6, 15),
				"Past due hostel fee");
		StudentFeeAssignment assignment = assignment(student, structure);

		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(hostelAllocationRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
				studentId,
				academicYear.getId(),
				HostelAllocationStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.HOSTEL))
				.thenReturn(List.of(assignment));

		service.reconcileHostelFees(studentId, academicYear.getId(), vacateDate, false);

		assertThat(assignment.getStatus()).isEqualTo(FeeAssignmentStatus.OVERDUE);
		assertThat(assignment.getBalanceAmount()).isEqualByComparingTo("1000.00");
	}

	@Test
	void reconcileTransportFeesCancelsFutureUnpaidObsoleteAssignmentAndAssignsCurrentFee() {
		UUID studentId = UUID.randomUUID();
		Student student = student(studentId);
		TransportVehicle vehicle = new TransportVehicle(academicYear, "BUS-1", "Bus 1", "BUS", 40, null, TransportStatus.ACTIVE);
		TransportRoute route = new TransportRoute(academicYear, "North Route", "NORTH", "A", "B", vehicle, TransportStatus.ACTIVE);
		TransportPickupPoint oldPoint = new TransportPickupPoint(route, "Old Stop", null, null, null, 1, TransportStatus.ACTIVE);
		TransportPickupPoint newPoint = new TransportPickupPoint(route, "New Stop", null, null, null, 2, TransportStatus.ACTIVE);
		setId(vehicle);
		setId(route);
		setId(oldPoint);
		setId(newPoint);
		LocalDate transferDate = LocalDate.of(2026, 9, 1);
		StudentTransportAssignment activeAssignment = new StudentTransportAssignment(
				student,
				academicYear,
				vehicle,
				route,
				newPoint,
				transferDate);
		setId(activeAssignment);
		FeeCategory category = category("TRANSPORT");
		FeeStructure oldStructure = transportFeeStructure(category, route, oldPoint, LocalDate.of(2026, 10, 15), "Old stop fee");
		FeeStructure currentStructure = transportFeeStructure(category, route, newPoint, LocalDate.of(2026, 10, 15), "New stop fee");
		StudentFeeAssignment obsoleteAssignment = assignment(student, oldStructure);
		TransportFeeStructure currentTransportFee = new TransportFeeStructure(
				academicYear,
				route,
				newPoint,
				category,
				currentStructure,
				money("750.00"),
				LocalDate.of(2026, 10, 15),
				false,
				1,
				FeeStructureStatus.ACTIVE);
		setId(currentTransportFee);

		when(academicHierarchyService.loadAcademicYear(academicYear.getId())).thenReturn(academicYear);
		when(transportAssignmentRepository.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
				studentId,
				academicYear.getId(),
				TransportStatus.ASSIGNED))
				.thenReturn(Optional.of(activeAssignment));
		when(transportFeeStructureRepository.findApplicable(
				academicYear.getId(),
				route.getId(),
				newPoint.getId(),
				FeeStructureStatus.ACTIVE))
				.thenReturn(List.of(currentTransportFee));
		when(assignmentRepository.findByStudentIdAndAcademicYearEntityIdAndSourceTypeAndDeletedFalseOrderByAssignedDateDesc(
				studentId,
				academicYear.getId(),
				com.school.erp.modules.fees.domain.FeeScope.TRANSPORT))
				.thenReturn(List.of(obsoleteAssignment));
		when(feeService.assignActiveTransportFeesToStudent(
				studentId,
				List.of(currentStructure.getId()),
				transferDate))
				.thenReturn(List.of());

		service.reconcileTransportFees(studentId, academicYear.getId(), transferDate);

		assertThat(obsoleteAssignment.getStatus()).isEqualTo(FeeAssignmentStatus.CANCELLED);
		assertThat(obsoleteAssignment.getBalanceAmount()).isZero();
		verify(feeService).assignActiveTransportFeesToStudent(
				studentId,
				List.of(currentStructure.getId()),
				transferDate);
	}

	private void setId(Object entity) {
		setId(entity, UUID.randomUUID());
	}

	private void setId(Object entity, UUID id) {
		ReflectionTestUtils.setField(entity, "id", id);
	}

	private void setIds(FeeStructure structure) {
		setId(structure);
		structure.getItems().forEach(this::setId);
		structure.getInstallments().forEach(this::setId);
	}

	private void setIds(StudentFeeAssignment assignment) {
		setId(assignment);
		assignment.getInstallments().forEach(this::setId);
		assignment.getDiscounts().forEach(this::setId);
		assignment.getPayments().forEach(payment -> {
			setId(payment);
			payment.getAllocations().forEach(this::setId);
		});
	}

	private Student student(UUID studentId) {
		Student student = new Student(
				"ADM-" + studentId.toString().substring(0, 8),
				"Aarav",
				LocalDate.of(2014, 5, 12),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		setId(student, studentId);
		return student;
	}

	private Hostel hostel() {
		Hostel hostel = new Hostel("MAIN", "Main Hostel", null);
		setId(hostel);
		return hostel;
	}

	private HostelRoom room(Hostel hostel, String roomNumber, String roomType) {
		HostelRoom room = new HostelRoom(hostel, roomNumber, roomType, 1, false);
		setId(room);
		return room;
	}

	private FeeCategory category(String code) {
		FeeCategory category = new FeeCategory(code, code + " Fee", "Test fee", 1);
		setId(category);
		return category;
	}

	private FeeStructure hostelFeeStructure(
			FeeCategory category,
			Hostel hostel,
			HostelRoom room,
			LocalDate dueDate,
			String name) {
		FeeStructure structure = new FeeStructure("2026-2027", "HOSTEL-" + hostel.getCode(), room.getRoomNumber(), name, null);
		structure.updateHostelMapping(academicYear, hostel, room, room.getRoomType());
		structure.addItem(category, money("1000.00"), true, 1);
		structure.addInstallment(1, "Hostel Fee", dueDate, money("1000.00"));
		structure.activate();
		setIds(structure);
		return structure;
	}

	private FeeStructure transportFeeStructure(
			FeeCategory category,
			TransportRoute route,
			TransportPickupPoint pickupPoint,
			LocalDate dueDate,
			String name) {
		FeeStructure structure = new FeeStructure("2026-2027", "TRANSPORT-" + route.getRouteCode(), pickupPoint.getPointName(), name, null);
		structure.updateTransportMapping(academicYear, route, pickupPoint);
		structure.addItem(category, money("750.00"), true, 1);
		structure.addInstallment(1, "Transport Fee", dueDate, money("750.00"));
		structure.activate();
		setIds(structure);
		return structure;
	}

	private StudentFeeAssignment assignment(Student student, FeeStructure structure) {
		StudentFeeAssignment assignment = new StudentFeeAssignment(
				student,
				structure,
				LocalDate.of(2026, 4, 1),
				null);
		structure.orderedInstallments().forEach(installment -> assignment.addInstallment(
				installment.getSequenceNo(),
				installment.getTitle(),
				installment.getDueDate(),
				installment.getAmount()));
		setIds(assignment);
		return assignment;
	}

	private BigDecimal money(String value) {
		return new BigDecimal(value);
	}
}
