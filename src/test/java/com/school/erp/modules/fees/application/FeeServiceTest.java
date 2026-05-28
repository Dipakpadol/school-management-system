package com.school.erp.modules.fees.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.fees.api.dto.AssessLateFeeRequest;
import com.school.erp.modules.fees.api.dto.FeeDiscountRequest;
import com.school.erp.modules.fees.api.dto.FeeReceiptResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureInstallmentRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureItemRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureResponse;
import com.school.erp.modules.fees.api.dto.PaymentCollectionRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.domain.DiscountCalculationType;
import com.school.erp.modules.fees.domain.DiscountType;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeReceipt;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.LateFeeCalculationType;
import com.school.erp.modules.fees.domain.LateFeeRule;
import com.school.erp.modules.fees.domain.PaymentMode;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeePaymentRepository;
import com.school.erp.modules.fees.infrastructure.FeeReceiptRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.LateFeeRuleRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

	@Mock
	private FeeCategoryRepository feeCategoryRepository;

	@Mock
	private FeeStructureRepository feeStructureRepository;

	@Mock
	private StudentFeeAssignmentRepository assignmentRepository;

	@Mock
	private LateFeeRuleRepository lateFeeRuleRepository;

	@Mock
	private FeeReceiptRepository feeReceiptRepository;

	@Mock
	private FeePaymentRepository feePaymentRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private AcademicHierarchyService academicHierarchyService;

	@Mock
	private AuditLogService auditLogService;

	private FeeService feeService;

	@BeforeEach
	void setUp() {
		feeService = new FeeService(
				feeCategoryRepository,
				feeStructureRepository,
				assignmentRepository,
				lateFeeRuleRepository,
				feeReceiptRepository,
				feePaymentRepository,
				studentRepository,
				academicHierarchyService,
				new FeeMapper(),
				auditLogService);
	}

	@Test
	void createFeeStructureBuildsItemsInstallmentsAndActivatesStructure() {
		FeeCategory tuition = category("TUITION");
		when(feeStructureRepository.existsActiveStructureForClass("2026-2027", "Class 6", "A")).thenReturn(false);
		when(feeCategoryRepository.findByIdAndDeletedFalse(tuition.getId())).thenReturn(Optional.of(tuition));
		when(feeStructureRepository.save(any(FeeStructure.class))).thenAnswer(invocation -> {
			FeeStructure structure = invocation.getArgument(0);
			setIds(structure);
			return structure;
		});

		FeeStructureResponse response = feeService.createFeeStructure(structureRequest(tuition.getId(), true));

		assertThat(response.status().name()).isEqualTo("ACTIVE");
		assertThat(response.totalAmount()).isEqualByComparingTo("30000.00");
		assertThat(response.items()).hasSize(1);
		assertThat(response.installments()).hasSize(2);

		ArgumentCaptor<FeeStructure> captor = ArgumentCaptor.forClass(FeeStructure.class);
		verify(feeStructureRepository).save(captor.capture());
		assertThat(captor.getValue().installmentTotal()).isEqualByComparingTo("30000.00");
	}

	@Test
	void createFeeStructureRejectsMismatchedItemAndInstallmentTotals() {
		UUID categoryId = UUID.randomUUID();
		FeeStructureRequest request = new FeeStructureRequest(
				"2026-2027",
				"Class 6",
				"A",
				"Class 6 Annual Fee",
				null,
				true,
				List.of(new FeeStructureItemRequest(categoryId, money("30000.00"), true, 1)),
				List.of(new FeeStructureInstallmentRequest(1, "First Term", LocalDate.of(2026, 6, 15), money("12000.00"))));

		assertThatThrownBy(() -> feeService.createFeeStructure(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void createFeeStructureRejectsDuplicateActiveClassSectionStructure() {
		FeeCategory tuition = category("TUITION");
		when(feeStructureRepository.existsActiveStructureForClass("2026-2027", "Class 6", "A")).thenReturn(true);

		assertThatThrownBy(() -> feeService.createFeeStructure(structureRequest(tuition.getId(), true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CONFLICT);
	}

	@Test
	void updateFeeStructureReplacesLinesWhenStructureIsUnassigned() {
		FeeCategory tuition = category("TUITION");
		FeeCategory transport = category("TRANSPORT");
		FeeStructure structure = activeStructure(tuition);
		FeeStructureRequest request = new FeeStructureRequest(
				"2026-2027",
				"Class 7",
				"B",
				"Class 7 Fee",
				"Updated annual fee structure",
				true,
				List.of(new FeeStructureItemRequest(transport.getId(), money("24000.00"), true, 1)),
				List.of(
						new FeeStructureInstallmentRequest(1, "First Term", LocalDate.of(2026, 6, 15), money("12000.00")),
						new FeeStructureInstallmentRequest(2, "Second Term", LocalDate.of(2026, 10, 15), money("12000.00"))));
		when(feeStructureRepository.findDetailedByIdAndDeletedFalse(structure.getId())).thenReturn(Optional.of(structure));
		when(assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structure.getId())).thenReturn(false);
		when(feeStructureRepository.existsStructureForClassExcludingId(
				structure.getId(),
				"2026-2027",
				"Class 7",
				"B")).thenReturn(false);
		when(feeCategoryRepository.findByIdAndDeletedFalse(transport.getId())).thenReturn(Optional.of(transport));

		FeeStructureResponse response = feeService.updateFeeStructure(structure.getId(), request);

		assertThat(response.className()).isEqualTo("Class 7");
		assertThat(response.sectionName()).isEqualTo("B");
		assertThat(response.name()).isEqualTo("Class 7 Fee");
		assertThat(response.totalAmount()).isEqualByComparingTo("24000.00");
		assertThat(response.items()).hasSize(1);
		assertThat(response.installments()).hasSize(2);
	}

	@Test
	void updateFeeStructureRejectsAssignedStructure() {
		FeeCategory tuition = category("TUITION");
		FeeStructure structure = activeStructure(tuition);
		when(feeStructureRepository.findDetailedByIdAndDeletedFalse(structure.getId())).thenReturn(Optional.of(structure));
		when(assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structure.getId())).thenReturn(true);

		assertThatThrownBy(() -> feeService.updateFeeStructure(structure.getId(), structureRequest(tuition.getId(), true)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void deleteCategoryRejectsCategoryUsedInStructure() {
		FeeCategory tuition = category("TUITION");
		when(feeCategoryRepository.findByIdAndDeletedFalse(tuition.getId())).thenReturn(Optional.of(tuition));
		when(feeStructureRepository.existsByCategoryId(tuition.getId())).thenReturn(true);

		assertThatThrownBy(() -> feeService.deleteCategory(tuition.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void deleteFeeStructureSoftDeletesUnassignedStructure() {
		FeeStructure structure = activeStructure(category("TUITION"));
		when(feeStructureRepository.findDetailedByIdAndDeletedFalse(structure.getId())).thenReturn(Optional.of(structure));
		when(assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structure.getId())).thenReturn(false);

		FeeStructureResponse response = feeService.deleteFeeStructure(structure.getId());

		assertThat(response.id()).isEqualTo(structure.getId());
		assertThat(structure.isDeleted()).isTrue();
	}

	@Test
	void deleteFeeStructureRejectsAssignedStructure() {
		FeeStructure structure = activeStructure(category("TUITION"));
		when(feeStructureRepository.findDetailedByIdAndDeletedFalse(structure.getId())).thenReturn(Optional.of(structure));
		when(assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structure.getId())).thenReturn(true);

		assertThatThrownBy(() -> feeService.deleteFeeStructure(structure.getId()))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void assignFeeToStudentCreatesStudentInstallments() {
		Student student = student();
		FeeStructure structure = activeStructure(category("TUITION"));
		when(studentRepository.findByIdAndDeletedFalse(student.getId())).thenReturn(Optional.of(student));
		when(feeStructureRepository.findDetailedByIdAndDeletedFalse(structure.getId())).thenReturn(Optional.of(structure));
		when(assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(student.getId(), structure.getId()))
				.thenReturn(false);
		when(assignmentRepository.save(any(StudentFeeAssignment.class))).thenAnswer(invocation -> {
			StudentFeeAssignment assignment = invocation.getArgument(0);
			setIds(assignment);
			return assignment;
		});

		StudentFeeAssignmentResponse response = feeService.assignFeeToStudent(new StudentFeeAssignmentRequest(
				student.getId(),
				structure.getId(),
				LocalDate.of(2026, 4, 1),
				"Assigned during admission."));

		assertThat(response.studentId()).isEqualTo(student.getId());
		assertThat(response.installments()).hasSize(2);
		assertThat(response.grossAmount()).isEqualByComparingTo("30000.00");
		assertThat(response.balanceAmount()).isEqualByComparingTo("30000.00");
	}

	@Test
	void applyDiscountAndCollectPaymentGeneratesReceiptAndAllocatesPayment() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		when(feeReceiptRepository.existsByReceiptNumberAndDeletedFalse(anyString())).thenReturn(false);
		when(feeReceiptRepository.save(any(FeeReceipt.class))).thenAnswer(invocation -> {
			FeeReceipt receipt = invocation.getArgument(0);
			setId(receipt);
			return receipt;
		});

		StudentFeeAssignmentResponse discounted = feeService.applyDiscount(
				assignment.getId(),
				new FeeDiscountRequest(
						null,
						DiscountType.SCHOLARSHIP,
						DiscountCalculationType.FLAT,
						money("1000.00"),
						"Merit scholarship",
						"principal@school.test"));

		assertThat(discounted.discountAmount()).isEqualByComparingTo("1000.00");
		assertThat(discounted.balanceAmount()).isEqualByComparingTo("29000.00");

		FeeReceiptResponse receipt = feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("29000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.CASH,
						null,
						"Rajesh Sharma",
						"accountant@school.test",
						"Full payment after discount.",
						false));

		assertThat(receipt.receiptNumber()).startsWith("RCPT-");
		assertThat(receipt.totalAmount()).isEqualByComparingTo("29000.00");
		assertThat(assignment.getStatus()).isEqualTo(FeeAssignmentStatus.PAID);
		assertThat(assignment.getPayments()).hasSize(1);
		assertThat(assignment.getPayments().iterator().next().getAllocations()).hasSize(2);
	}

	@Test
	void collectPaymentRequiresReferenceForNonCashPayment() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));

		assertThatThrownBy(() -> feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.UPI,
						null,
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void collectPaymentRejectsDuplicateUpiReferenceWithBusinessError() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		when(feePaymentRepository.existsByPaymentModeAndReferenceNumberAndDeletedFalse(PaymentMode.UPI, "UPI-123"))
				.thenReturn(true);

		assertThatThrownBy(() -> feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.UPI,
						"UPI-123",
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Reference number already exists for this payment mode.")
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
		verify(feeReceiptRepository, never()).save(any());
	}

	@Test
	void collectPaymentAllowsNewUpiReference() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		when(feePaymentRepository.existsByPaymentModeAndReferenceNumberAndDeletedFalse(PaymentMode.UPI, "UPI-NEW"))
				.thenReturn(false);
		stubReceiptSave();

		FeeReceiptResponse receipt = feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.UPI,
						"UPI-NEW",
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false));

		assertThat(receipt.receiptNumber()).startsWith("RCPT-");
		assertThat(assignment.getPayments()).hasSize(1);
		assertThat(assignment.getPayments().iterator().next().getReferenceNumber()).isEqualTo("UPI-NEW");
	}

	@Test
	void collectPaymentAllowsCashWithoutReference() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		stubReceiptSave();

		FeeReceiptResponse receipt = feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.CASH,
						null,
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false));

		assertThat(receipt.receiptNumber()).startsWith("RCPT-");
		assertThat(assignment.getPayments()).hasSize(1);
		assertThat(assignment.getPayments().iterator().next().getReferenceNumber()).isNull();
	}

	@Test
	void collectPaymentRejectsBankTransferWithoutReference() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));

		assertThatThrownBy(() -> feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.BANK_TRANSFER,
						" ",
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false)))
				.isInstanceOf(BusinessException.class)
				.hasMessage("Reference number is required for non-cash payments.")
				.extracting("errorCode")
				.isEqualTo(ErrorCode.VALIDATION_ERROR);
	}

	@Test
	void collectPaymentAllowsReferenceUsedOnlyByDeletedPayment() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		when(feePaymentRepository.existsByPaymentModeAndReferenceNumberAndDeletedFalse(PaymentMode.UPI, "SOFT-DELETED-REF"))
				.thenReturn(false);
		stubReceiptSave();

		FeeReceiptResponse receipt = feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("5000.00"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.UPI,
						"SOFT-DELETED-REF",
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false));

		assertThat(receipt.receiptNumber()).startsWith("RCPT-");
		assertThat(assignment.getPayments()).hasSize(1);
	}

	@Test
	void collectPaymentRejectsOverpayment() {
		StudentFeeAssignment assignment = assignment();
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));

		assertThatThrownBy(() -> feeService.collectPayment(
				assignment.getId(),
				new PaymentCollectionRequest(
						money("30000.01"),
						LocalDate.of(2026, 6, 20),
						PaymentMode.CASH,
						null,
						"Rajesh Sharma",
						"accountant@school.test",
						null,
						false)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void assessLateFeesAppliesMatchingRule() {
		StudentFeeAssignment assignment = assignment();
		LateFeeRule rule = new LateFeeRule(
				"Standard late fee",
				"2026-2027",
				"Class 6",
				"A",
				0,
				LateFeeCalculationType.PER_DAY,
				money("10.00"),
				money("500.00"),
				true);
		when(assignmentRepository.findDetailedByIdAndDeletedFalse(assignment.getId())).thenReturn(Optional.of(assignment));
		when(lateFeeRuleRepository.findByAcademicYearIgnoreCaseAndActiveTrueAndDeletedFalse("2026-2027"))
				.thenReturn(List.of(rule));

		StudentFeeAssignmentResponse response = feeService.assessLateFees(
				assignment.getId(),
				new AssessLateFeeRequest(LocalDate.of(2026, 6, 20)));

		assertThat(response.lateFeeAmount()).isEqualByComparingTo("50.00");
		assertThat(response.balanceAmount()).isEqualByComparingTo("30050.00");
	}

	private FeeStructureRequest structureRequest(UUID categoryId, boolean activate) {
		return new FeeStructureRequest(
				"2026-2027",
				"Class 6",
				"A",
				"Class 6 Annual Fee",
				"Annual fee structure",
				activate,
				List.of(new FeeStructureItemRequest(categoryId, money("30000.00"), true, 1)),
				List.of(
						new FeeStructureInstallmentRequest(1, "First Term", LocalDate.of(2026, 6, 15), money("15000.00")),
						new FeeStructureInstallmentRequest(2, "Second Term", LocalDate.of(2026, 10, 15), money("15000.00"))));
	}

	private StudentFeeAssignment assignment() {
		Student student = student();
		FeeStructure structure = activeStructure(category("TUITION"));
		StudentFeeAssignment assignment = new StudentFeeAssignment(student, structure, LocalDate.of(2026, 4, 1), null);
		structure.orderedInstallments().forEach(installment -> assignment.addInstallment(
				installment.getSequenceNo(),
				installment.getTitle(),
				installment.getDueDate(),
				installment.getAmount()));
		setIds(assignment);
		return assignment;
	}

	private FeeStructure activeStructure(FeeCategory category) {
		FeeStructure structure = new FeeStructure("2026-2027", "Class 6", "A", "Class 6 Annual Fee", null);
		structure.addItem(category, money("30000.00"), true, 1);
		structure.addInstallment(1, "First Term", LocalDate.of(2026, 6, 15), money("15000.00"));
		structure.addInstallment(2, "Second Term", LocalDate.of(2026, 10, 15), money("15000.00"));
		structure.activate();
		setIds(structure);
		return structure;
	}

	private FeeCategory category(String code) {
		FeeCategory category = new FeeCategory(code, code + " Fee", "Test fee", 1);
		setId(category);
		return category;
	}

	private void stubReceiptSave() {
		when(feeReceiptRepository.existsByReceiptNumberAndDeletedFalse(anyString())).thenReturn(false);
		when(feeReceiptRepository.save(any(FeeReceipt.class))).thenAnswer(invocation -> {
			FeeReceipt receipt = invocation.getArgument(0);
			setId(receipt);
			return receipt;
		});
	}

	private Student student() {
		Student student = new Student(
				"ADM-2026-0001",
				"Aarav",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				LocalDate.of(2026, 4, 1));
		student.updateProfile(
				"Aarav",
				"Kumar",
				"Sharma",
				LocalDate.of(2014, 8, 17),
				Gender.MALE,
				"B+",
				"aarav.sharma@student.school.test",
				"+919876543210",
				LocalDate.of(2026, 4, 1),
				null,
				null,
				null,
				"Bengaluru",
				"Karnataka",
				"560001",
				"India");
		setId(student);
		return student;
	}

	private BigDecimal money(String value) {
		return new BigDecimal(value);
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

	private void setId(Object entity) {
		ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
	}
}
