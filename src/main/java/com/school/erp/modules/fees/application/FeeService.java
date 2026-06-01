package com.school.erp.modules.fees.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.academic.domain.ClassEntity;
import com.school.erp.modules.fees.api.dto.AssessLateFeeRequest;
import com.school.erp.modules.fees.api.dto.ClassFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.ClassFeeAssignmentResponse;
import com.school.erp.modules.fees.api.dto.ClassStudentFeeResponse;
import com.school.erp.modules.fees.api.dto.DefaulterSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeAssignmentSearchRequest;
import com.school.erp.modules.fees.api.dto.FeeCategoryRequest;
import com.school.erp.modules.fees.api.dto.FeeCategoryResponse;
import com.school.erp.modules.fees.api.dto.FeeDefaulterResponse;
import com.school.erp.modules.fees.api.dto.FeeDiscountRequest;
import com.school.erp.modules.fees.api.dto.FeeReceiptResponse;
import com.school.erp.modules.fees.api.dto.FeeReportRequest;
import com.school.erp.modules.fees.api.dto.FeeReportSummaryResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureRequest;
import com.school.erp.modules.fees.api.dto.FeeStructureResponse;
import com.school.erp.modules.fees.api.dto.LateFeeRuleRequest;
import com.school.erp.modules.fees.api.dto.LateFeeRuleResponse;
import com.school.erp.modules.fees.api.dto.PaymentCollectionRequest;
import com.school.erp.modules.fees.api.dto.PaymentActionRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentRequest;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeSummaryResponse;
import com.school.erp.modules.fees.domain.DiscountCalculationType;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeDiscount;
import com.school.erp.modules.fees.domain.FeeInstallmentStatus;
import com.school.erp.modules.fees.domain.FeePayment;
import com.school.erp.modules.fees.domain.FeePaymentStatus;
import com.school.erp.modules.fees.domain.FeeReceipt;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureInstallment;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.fees.domain.LateFeeRule;
import com.school.erp.modules.fees.domain.PaymentMode;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.domain.StudentFeeInstallment;
import com.school.erp.modules.fees.infrastructure.FeeAssignmentSpecifications;
import com.school.erp.modules.fees.infrastructure.FeeCategoryRepository;
import com.school.erp.modules.fees.infrastructure.FeePaymentRepository;
import com.school.erp.modules.fees.infrastructure.FeeReceiptRepository;
import com.school.erp.modules.fees.infrastructure.FeeStructureRepository;
import com.school.erp.modules.fees.infrastructure.LateFeeRuleRepository;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeService {

	private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final String MODULE_NAME = "FEES";

	private final FeeCategoryRepository feeCategoryRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentFeeAssignmentRepository assignmentRepository;
	private final LateFeeRuleRepository lateFeeRuleRepository;
	private final FeeReceiptRepository feeReceiptRepository;
	private final FeePaymentRepository feePaymentRepository;
	private final StudentRepository studentRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final FeeMapper feeMapper;
	private final AuditLogService auditLogService;

	@Transactional
	public FeeCategoryResponse createCategory(FeeCategoryRequest request) {
		String code = normalizeCode(request.code());
		if (feeCategoryRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Fee category already exists: " + code);
		}
		FeeCategory category = new FeeCategory(code, request.name(), request.description(), request.sortOrder());
		category.update(code, request.name(), request.description(), request.active(), request.sortOrder(), mandatory(request));
		FeeCategoryResponse response = feeMapper.toCategoryResponse(feeCategoryRepository.save(category));
		audit("FeeCategory", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<FeeCategoryResponse> listCategories(PageRequestDto pageRequest) {
		return PageResponse.from(
				feeCategoryRepository.findAllByDeletedFalse(pageRequest.toPageable("sortOrder")),
				feeMapper::toCategoryResponse);
	}

	@Transactional(readOnly = true)
	public FeeCategoryResponse getCategory(UUID categoryId) {
		return feeMapper.toCategoryResponse(loadCategory(categoryId));
	}

	@Transactional
	public FeeCategoryResponse updateCategory(UUID categoryId, FeeCategoryRequest request) {
		FeeCategory category = loadCategory(categoryId);
		FeeCategoryResponse oldValue = feeMapper.toCategoryResponse(category);
		String code = normalizeCode(request.code());
		feeCategoryRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> !existing.getId().equals(categoryId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Fee category already exists: " + code);
				});
		category.update(code, request.name(), request.description(), request.active(), request.sortOrder(), mandatory(request));
		FeeCategoryResponse response = feeMapper.toCategoryResponse(category);
		audit("FeeCategory", categoryId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public FeeStructureResponse createFeeStructure(FeeStructureRequest request) {
		validateFeeStructureRequest(request);
		if (feeStructureRepository.existsActiveStructureForClass(
				request.academicYear(),
				request.className(),
				blankToNull(request.sectionName()))) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Fee structure already exists for academic year, class, and section.");
		}

		FeeStructure structure = new FeeStructure(
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.name(),
				request.description());
		ResolvedFeeStructureClass mapping = resolveFeeStructureClass(request.academicYear(), request.className());
		structure.updateAcademicMapping(mapping.academicYear(), mapping.classEntity());
		applyFeeStructureLines(structure, request);
		if (request.activate()) {
			structure.activate();
		}
		FeeStructureResponse response = feeMapper.toStructureResponse(feeStructureRepository.save(structure));
		audit("FeeStructure", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public FeeStructureResponse updateFeeStructure(UUID structureId, FeeStructureRequest request) {
		validateFeeStructureRequest(request);
		FeeStructure structure = loadStructure(structureId);
		FeeStructureResponse oldValue = feeMapper.toStructureResponse(structure);
		if (assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structureId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Assigned fee structures cannot be edited.");
		}
		if (feeStructureRepository.existsStructureForClassExcludingId(
				structureId,
				request.academicYear(),
				request.className(),
				blankToNull(request.sectionName()))) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Fee structure already exists for academic year, class, and section.");
		}

		structure.updateDetails(
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.name(),
				request.description());
		ResolvedFeeStructureClass mapping = resolveFeeStructureClass(request.academicYear(), request.className());
		structure.updateAcademicMapping(mapping.academicYear(), mapping.classEntity());
		String actor = currentActor();
		structure.clearItems(actor);
		structure.clearInstallments(actor);
		applyFeeStructureLines(structure, request);
		if (request.activate()) {
			structure.activate();
		}
		else if (structure.isActive()) {
			structure.deactivate();
		}
		FeeStructureResponse response = feeMapper.toStructureResponse(structure);
		audit("FeeStructure", structureId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public FeeStructureResponse getFeeStructure(UUID structureId) {
		return feeMapper.toStructureResponse(loadStructure(structureId));
	}

	@Transactional(readOnly = true)
	public PageResponse<FeeStructureResponse> listFeeStructures(PageRequestDto pageRequest) {
		return PageResponse.from(
				feeStructureRepository.findAllByDeletedFalse(pageRequest.toPageable("academicYear")),
				feeMapper::toStructureResponse);
	}

	@Transactional(readOnly = true)
	public PageResponse<FeeStructureResponse> listFeeStructures(
			UUID academicYearId,
			UUID classId,
			PageRequestDto pageRequest) {
		if (academicYearId == null && classId == null) {
			return listFeeStructures(pageRequest);
		}
		AcademicYear academicYear = academicYearId == null
				? null
				: academicHierarchyService.loadAcademicYear(academicYearId);
		ClassEntity classEntity = classId == null
				? null
				: academicHierarchyService.loadClass(classId);
		var pageable = pageRequest.toPageable("academicYear");
		if (academicYearId != null && classId != null) {
			return PageResponse.from(
					feeStructureRepository.findByAcademicYearAndClassMappingOrLegacy(
							academicYearId,
							academicYear.getName(),
							academicYear.getCode(),
							classId,
							classEntity.getName(),
							classEntity.getCode(),
							pageable),
					feeMapper::toStructureResponse);
		}
		if (academicYearId != null) {
			return PageResponse.from(
					feeStructureRepository.findByAcademicYearEntityIdAndDeletedFalse(academicYearId, pageable),
					feeMapper::toStructureResponse);
		}
		return PageResponse.from(
				feeStructureRepository.findByClassEntityIdAndDeletedFalse(classId, pageable),
				feeMapper::toStructureResponse);
	}

	@Transactional
	public StudentFeeAssignmentResponse assignFeeToStudent(StudentFeeAssignmentRequest request) {
		Student student = studentRepository.findByIdAndDeletedFalse(request.studentId())
				.orElseThrow(() -> new ResourceNotFoundException("Student", request.studentId()));
		FeeStructure structure = loadStructure(request.feeStructureId());
		if (!structure.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active fee structures can be assigned.");
		}
		if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(student.getId(), structure.getId())) {
			throw new BusinessException(ErrorCode.CONFLICT, "Fee structure is already assigned to this student.");
		}

		StudentFeeAssignment assignment = buildAssignment(student, structure, request.assignedDate(), request.notes());
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignmentRepository.save(assignment));
		audit("StudentFeeAssignment", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public List<StudentFeeAssignmentResponse> assignActiveClassFeesToStudent(
			UUID studentId,
			UUID classId,
			LocalDate assignedDate) {
		Student student = studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		academicHierarchyService.loadClass(classId);
		List<StudentFeeAssignmentResponse> created = new java.util.ArrayList<>();
		for (FeeStructure structure : feeStructureRepository
				.findByClassEntityIdAndStatusAndDeletedFalseOrderByCreatedAtAsc(classId, FeeStructureStatus.ACTIVE)) {
			if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(studentId, structure.getId())) {
				continue;
			}
			StudentFeeAssignment assignment = buildAssignment(student, structure, assignedDate, "Auto assigned from class fee structure.");
			StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignmentRepository.save(assignment));
			created.add(response);
			audit("StudentFeeAssignment", response.id(), "AUTO_CLASS_FEE_ASSIGNED", null, response);
		}
		return created;
	}

	@Transactional(readOnly = true)
	public List<ClassStudentFeeResponse> studentsForClass(UUID classId) {
		ClassEntity classEntity = academicHierarchyService.loadClass(classId);
		return studentRepository.findActiveStudentsByClassId(classId).stream()
				.map(student -> toClassStudentFeeResponse(student, classEntity))
				.toList();
	}

	@Transactional
	public ClassFeeAssignmentResponse assignFeeToClass(UUID classId, ClassFeeAssignmentRequest request) {
		ClassEntity classEntity = academicHierarchyService.loadClass(classId);
		if (request.classId() != null && !request.classId().equals(classId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Class in request does not match the selected class.");
		}
		if (request.academicYearId() != null
				&& !request.academicYearId().equals(classEntity.getAcademicYear().getId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Academic year in request does not match the selected class.");
		}
		List<UUID> feeStructureIds = feeStructureIds(request);
		List<Student> students = studentRepository.findActiveStudentsByClassId(classId);
		List<StudentFeeAssignmentResponse> created = new ArrayList<>();
		List<UUID> assignedFeeStructures = new ArrayList<>();
		int skipped = 0;
		for (UUID feeStructureId : feeStructureIds) {
			FeeStructure structure = loadStructure(feeStructureId);
			validateClassFeeStructure(classId, classEntity, structure);
			if (structure.getAcademicYearEntity() == null || structure.getClassEntity() == null) {
				structure.updateAcademicMapping(classEntity.getAcademicYear(), classEntity);
			}
			assignedFeeStructures.add(structure.getId());
			for (Student student : students) {
				if (assignmentRepository.existsByStudentIdAndFeeStructureIdAndDeletedFalse(student.getId(), structure.getId())) {
					if (!request.skipExisting()) {
						throw new BusinessException(
								ErrorCode.CONFLICT,
								"Fee structure is already assigned to one or more students in this class.");
					}
					skipped++;
					continue;
				}
				StudentFeeAssignment assignment = buildAssignment(student, structure, request.assignedDate(), request.notes());
				StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignmentRepository.save(assignment));
				created.add(response);
			}
		}
		String message = students.isEmpty()
				? "Fee assigned to class. No students found currently."
				: "Class fee assigned successfully.";
		ClassFeeAssignmentResponse response = new ClassFeeAssignmentResponse(
				classEntity.getAcademicYear().getId(),
				classId,
				assignedFeeStructures.get(0),
				assignedFeeStructures,
				students.size(),
				created.size(),
				skipped,
				created.size(),
				skipped,
				created,
				message);
		audit(
				"ClassFeeAssignment",
				classId,
				"CLASS_FEE_ASSIGNED",
				null,
				response);
		return response;
	}

	private void validateClassFeeStructure(UUID classId, ClassEntity classEntity, FeeStructure structure) {
		if (!structure.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active fee structures can be assigned.");
		}
		if (structure.getClassEntity() != null && !structure.getClassEntity().getId().equals(classId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee structure does not belong to the selected class.");
		}
		if (structure.getClassEntity() == null
				&& !matchesIgnoringCase(structure.getClassName(), classEntity.getName(), classEntity.getCode())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee structure class name does not match the selected class.");
		}
		if (structure.getAcademicYearEntity() != null
				&& !structure.getAcademicYearEntity().getId().equals(classEntity.getAcademicYear().getId())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee structure academic year does not match the selected class.");
		}
		if (structure.getAcademicYearEntity() == null
				&& !matchesIgnoringCase(
						structure.getAcademicYear(),
						classEntity.getAcademicYear().getName(),
						classEntity.getAcademicYear().getCode())) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee structure academic year does not match the selected class.");
		}
	}

	private List<UUID> feeStructureIds(ClassFeeAssignmentRequest request) {
		Set<UUID> ids = new LinkedHashSet<>();
		if (request.feeStructureId() != null) {
			ids.add(request.feeStructureId());
		}
		if (request.feeStructureIds() != null) {
			ids.addAll(request.feeStructureIds().stream()
					.filter(java.util.Objects::nonNull)
					.toList());
		}
		if (ids.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "At least one fee structure is required.");
		}
		return List.copyOf(ids);
	}

	@Transactional(readOnly = true)
	public StudentFeeSummaryResponse studentFeeSummary(UUID studentId) {
		Student student = studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		List<StudentFeeAssignmentResponse> assignments = assignmentRepository
				.findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(studentId)
				.stream()
				.map(feeMapper::toAssignmentResponse)
				.toList();
		BigDecimal gross = assignments.stream().map(StudentFeeAssignmentResponse::grossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal discount = assignments.stream().map(StudentFeeAssignmentResponse::discountAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal lateFee = assignments.stream().map(StudentFeeAssignmentResponse::lateFeeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal paid = assignments.stream().map(StudentFeeAssignmentResponse::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal balance = assignments.stream().map(StudentFeeAssignmentResponse::balanceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new StudentFeeSummaryResponse(
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				gross,
				discount,
				lateFee,
				paid,
				balance,
				assignments);
	}

	@Transactional(readOnly = true)
	public List<com.school.erp.modules.fees.api.dto.FeePaymentResponse> studentPaymentHistory(UUID studentId) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		return assignmentRepository.findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(studentId).stream()
				.flatMap(assignment -> feeMapper.toAssignmentResponse(assignment).payments().stream())
				.sorted(Comparator.comparing(com.school.erp.modules.fees.api.dto.FeePaymentResponse::paymentDate).reversed())
				.toList();
	}

	@Transactional
	public FeeReceiptResponse collectStudentPayment(UUID studentId, PaymentCollectionRequest request) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		StudentFeeAssignment assignment = assignmentRepository.findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(studentId).stream()
				.filter(existing -> existing.getBalanceAmount().signum() > 0)
				.findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Open fee assignment for student", studentId));
		return collectPayment(assignment.getId(), request);
	}

	@Transactional(readOnly = true)
	public StudentFeeAssignmentResponse getAssignment(UUID assignmentId) {
		return feeMapper.toAssignmentResponse(loadAssignment(assignmentId));
	}

	@Transactional(readOnly = true)
	public PageResponse<StudentFeeAssignmentResponse> searchAssignments(
			FeeAssignmentSearchRequest request,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				assignmentRepository.findAll(
						FeeAssignmentSpecifications.matching(request),
						pageRequest.toPageable("assignedDate")),
				feeMapper::toAssignmentResponse);
	}

	@Transactional
	public StudentFeeAssignmentResponse applyDiscount(UUID assignmentId, FeeDiscountRequest request) {
		StudentFeeAssignment assignment = loadAssignment(assignmentId);
		StudentFeeAssignmentResponse oldValue = feeMapper.toAssignmentResponse(assignment);
		StudentFeeInstallment installment = request.installmentId() == null
				? null
				: assignment.findInstallment(request.installmentId())
						.orElseThrow(() -> new ResourceNotFoundException("Fee installment", request.installmentId()));
		BigDecimal basis = installment == null ? assignment.getBalanceAmount() : installment.getBalanceAmount();
		BigDecimal amount = calculateDiscountAmount(request.calculationType(), request.value(), basis);
		if (amount.compareTo(basis) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Discount cannot exceed outstanding balance.");
		}
		FeeDiscount discount = new FeeDiscount(
				assignment,
				installment,
				request.discountType(),
				request.calculationType(),
				request.value(),
				amount,
				request.reason(),
				request.approvedBy());
		assignment.applyDiscount(discount);
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignment);
		audit("StudentFeeAssignment", assignmentId, "DISCOUNT_APPLIED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentFeeAssignmentResponse assessLateFees(UUID assignmentId, AssessLateFeeRequest request) {
		StudentFeeAssignment assignment = loadAssignment(assignmentId);
		StudentFeeAssignmentResponse oldValue = feeMapper.toAssignmentResponse(assignment);
		assignment.assessLateFees(matchingLateFeeRules(assignment), defaultDate(request.asOf()));
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignment);
		audit("StudentFeeAssignment", assignmentId, "LATE_FEE_ASSESSED", oldValue, response);
		return response;
	}

	@Transactional
	public FeeReceiptResponse collectPayment(UUID assignmentId, PaymentCollectionRequest request) {
		StudentFeeAssignment assignment = loadAssignment(assignmentId);
		StudentFeeAssignmentResponse oldAssignment = feeMapper.toAssignmentResponse(assignment);
		if (request.assessLateFee()) {
			assignment.assessLateFees(matchingLateFeeRules(assignment), request.paymentDate());
		}
		BigDecimal amount = money(request.amount());
		if (amount.compareTo(assignment.getBalanceAmount()) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payment amount cannot exceed outstanding balance.");
		}
		validatePaymentReference(request.paymentMode(), request.referenceNumber());

		FeeReceipt receipt = feeReceiptRepository.save(new FeeReceipt(
				generateReceiptNumber(),
				assignment.getStudent(),
				assignment,
				amount,
				request.payerName(),
				request.paymentMode()));
		FeePayment payment = new FeePayment(
				assignment,
				receipt,
				amount,
				request.paymentDate(),
				request.paymentMode(),
				request.referenceNumber(),
				request.payerName(),
				firstText(request.collectedBy(), currentActor()),
				request.remarks());
		assignment.collectPayment(payment);
		FeeReceiptResponse receiptResponse = feeMapper.toReceiptResponse(receipt);
		StudentFeeAssignmentResponse newAssignment = feeMapper.toAssignmentResponse(assignment);
		audit(
				"FeeReceipt",
				receiptResponse.id(),
				"PAYMENT_COLLECTED",
				oldAssignment,
				Map.of("receipt", receiptResponse, "assignment", newAssignment));
		return receiptResponse;
	}

	@Transactional(readOnly = true)
	public FeeReceiptResponse getReceipt(String receiptNumber) {
		return feeMapper.toReceiptResponse(feeReceiptRepository.findByReceiptNumberAndDeletedFalse(receiptNumber)
				.orElseThrow(() -> new ResourceNotFoundException("Fee receipt", receiptNumber)));
	}

	@Transactional(readOnly = true)
	public FeeReceiptResponse getPaymentReceipt(UUID paymentId) {
		FeePayment payment = feePaymentRepository.findDetailedByIdAndDeletedFalse(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee payment", paymentId));
		return feeMapper.toReceiptResponse(payment.getReceipt());
	}

	@Transactional
	public StudentFeeAssignmentResponse reversePayment(UUID paymentId, PaymentActionRequest request) {
		return processPaymentAction(paymentId, request, FeePaymentStatus.REVERSED, "PAYMENT_REVERSAL");
	}

	@Transactional
	public StudentFeeAssignmentResponse voidPayment(UUID paymentId, PaymentActionRequest request) {
		return processPaymentAction(paymentId, request, FeePaymentStatus.VOIDED, "PAYMENT_VOID");
	}

	@Transactional
	public StudentFeeAssignmentResponse refundPayment(UUID paymentId, PaymentActionRequest request) {
		return processPaymentAction(paymentId, request, FeePaymentStatus.REFUNDED, "REFUND");
	}

	@Transactional
	public FeeCategoryResponse deleteCategory(UUID categoryId) {
		FeeCategory category = loadCategory(categoryId);
		FeeCategoryResponse oldValue = feeMapper.toCategoryResponse(category);
		if (feeStructureRepository.existsByCategoryId(categoryId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee category is already used in a fee structure.");
		}
		category.softDelete(currentActor());
		FeeCategoryResponse response = feeMapper.toCategoryResponse(category);
		audit("FeeCategory", categoryId, "DELETE", oldValue, Map.of("deleted", true, "categoryId", categoryId));
		return response;
	}

	@Transactional
	public FeeStructureResponse deleteFeeStructure(UUID structureId) {
		FeeStructure structure = loadStructure(structureId);
		FeeStructureResponse oldValue = feeMapper.toStructureResponse(structure);
		if (assignmentRepository.existsByFeeStructureIdAndDeletedFalse(structureId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Assigned fee structures cannot be deleted.");
		}
		structure.softDelete(currentActor());
		FeeStructureResponse response = feeMapper.toStructureResponse(structure);
		audit("FeeStructure", structureId, "DELETE", oldValue, Map.of("deleted", true, "structureId", structureId));
		return response;
	}

	@Transactional
	public StudentFeeAssignmentResponse deleteAssignment(UUID assignmentId) {
		StudentFeeAssignment assignment = loadAssignment(assignmentId);
		StudentFeeAssignmentResponse oldValue = feeMapper.toAssignmentResponse(assignment);
		assignment.softDelete(currentActor());
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignment);
		audit("StudentFeeAssignment", assignmentId, "DELETE", oldValue, Map.of("deleted", true, "assignmentId", assignmentId));
		return response;
	}

	@Transactional
	public StudentFeeAssignmentResponse cancelAssignment(UUID assignmentId) {
		StudentFeeAssignment assignment = loadAssignment(assignmentId);
		StudentFeeAssignmentResponse oldValue = feeMapper.toAssignmentResponse(assignment);
		assignment.cancel();
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignment);
		audit("StudentFeeAssignment", assignmentId, "STATUS_CHANGE", oldValue, response);
		return response;
	}

	@Transactional
	public LateFeeRuleResponse createLateFeeRule(LateFeeRuleRequest request) {
		LateFeeRule rule = new LateFeeRule(
				request.name(),
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.graceDays(),
				request.calculationType(),
				request.amount(),
				request.maxAmount(),
				request.active());
		LateFeeRuleResponse response = feeMapper.toLateFeeRuleResponse(lateFeeRuleRepository.save(rule));
		audit("LateFeeRule", response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public LateFeeRuleResponse getLateFeeRule(UUID ruleId) {
		return feeMapper.toLateFeeRuleResponse(loadLateFeeRule(ruleId));
	}

	@Transactional
	public LateFeeRuleResponse updateLateFeeRule(UUID ruleId, LateFeeRuleRequest request) {
		LateFeeRule rule = loadLateFeeRule(ruleId);
		LateFeeRuleResponse oldValue = feeMapper.toLateFeeRuleResponse(rule);
		rule.update(
				request.name(),
				request.academicYear(),
				request.className(),
				request.sectionName(),
				request.graceDays(),
				request.calculationType(),
				request.amount(),
				request.maxAmount(),
				request.active());
		LateFeeRuleResponse response = feeMapper.toLateFeeRuleResponse(rule);
		audit("LateFeeRule", ruleId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public LateFeeRuleResponse deleteLateFeeRule(UUID ruleId) {
		LateFeeRule rule = loadLateFeeRule(ruleId);
		LateFeeRuleResponse oldValue = feeMapper.toLateFeeRuleResponse(rule);
		rule.softDelete(currentActor());
		LateFeeRuleResponse response = feeMapper.toLateFeeRuleResponse(rule);
		audit("LateFeeRule", ruleId, "DELETE", oldValue, Map.of("deleted", true, "ruleId", ruleId));
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<LateFeeRuleResponse> listLateFeeRules(PageRequestDto pageRequest) {
		return PageResponse.from(
				lateFeeRuleRepository.findAllByDeletedFalse(pageRequest.toPageable("academicYear")),
				feeMapper::toLateFeeRuleResponse);
	}

	@Transactional(readOnly = true)
	public PageResponse<FeeDefaulterResponse> findDefaulters(DefaulterSearchRequest request, PageRequestDto pageRequest) {
		LocalDate asOf = defaultDate(request.asOf());
		BigDecimal minimumBalance = request.minimumBalance() == null ? BigDecimal.valueOf(0.01) : money(request.minimumBalance());
		return PageResponse.from(
				assignmentRepository.findDefaulters(
						asOf,
						blankToNull(request.academicYear()),
						blankToNull(request.className()),
						blankToNull(request.sectionName()),
						minimumBalance,
						FeeInstallmentStatus.PAID,
						FeeInstallmentStatus.CANCELLED,
						pageRequest.toPageable("balanceAmount")),
				assignment -> feeMapper.toDefaulterResponse(assignment, asOf));
	}

	@Transactional(readOnly = true)
	public FeeReportSummaryResponse summarizeFees(FeeReportRequest request) {
		return feeMapper.toReportSummary(assignmentRepository.summarize(
				blankToNull(request.academicYear()),
				blankToNull(request.className()),
				blankToNull(request.sectionName()),
				request.status()));
	}

	private StudentFeeAssignment buildAssignment(
			Student student,
			FeeStructure structure,
			LocalDate assignedDate,
			String notes) {
		StudentFeeAssignment assignment = new StudentFeeAssignment(student, structure, assignedDate, notes);
		for (FeeStructureInstallment installment : structure.orderedInstallments()) {
			assignment.addInstallment(
					installment.getSequenceNo(),
					installment.getTitle(),
					installment.getDueDate(),
					installment.getAmount());
		}
		return assignment;
	}

	private ClassStudentFeeResponse toClassStudentFeeResponse(Student student, ClassEntity classEntity) {
		StudentFeeAssignment assignment = assignmentRepository.findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(student.getId()).stream()
				.filter(existing -> assignmentBelongsToClass(existing, classEntity))
				.findFirst()
				.orElse(null);
		String rollNumber = student.getCurrentAssignment().map(current -> current.getRollNumber()).orElse(null);
		return new ClassStudentFeeResponse(
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				rollNumber,
				assignment == null ? null : assignment.getId(),
				assignment == null ? null : assignment.getFeeStructure().getId(),
				assignment == null ? null : assignment.getFeeStructure().getName(),
				assignment == null ? BigDecimal.ZERO : assignment.getGrossAmount(),
				assignment == null ? BigDecimal.ZERO : assignment.getDiscountAmount(),
				assignment == null ? BigDecimal.ZERO : assignment.getPaidAmount(),
				assignment == null ? BigDecimal.ZERO : assignment.getBalanceAmount(),
				assignment == null ? null : assignment.getStatus());
	}

	private boolean assignmentBelongsToClass(StudentFeeAssignment assignment, ClassEntity classEntity) {
		if (assignment.getClassEntity() != null) {
			return assignment.getClassEntity().getId().equals(classEntity.getId());
		}
		return matchesIgnoringCase(assignment.getClassName(), classEntity.getName(), classEntity.getCode());
	}

	private boolean matchesIgnoringCase(String candidate, String... expectedValues) {
		if (!StringUtils.hasText(candidate)) {
			return false;
		}
		for (String expectedValue : expectedValues) {
			if (StringUtils.hasText(expectedValue) && candidate.trim().equalsIgnoreCase(expectedValue.trim())) {
				return true;
			}
		}
		return false;
	}

	private ResolvedFeeStructureClass resolveFeeStructureClass(String academicYearValue, String classNameValue) {
		AcademicYear academicYear = academicHierarchyService.resolveAcademicYear(academicYearValue);
		ClassEntity classEntity = academicHierarchyService.resolveClass(academicYear, classNameValue);
		return new ResolvedFeeStructureClass(academicYear, classEntity);
	}

	private FeeCategory loadCategory(UUID categoryId) {
		return feeCategoryRepository.findByIdAndDeletedFalse(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee category", categoryId));
	}

	private FeeStructure loadStructure(UUID structureId) {
		return feeStructureRepository.findDetailedByIdAndDeletedFalse(structureId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee structure", structureId));
	}

	private LateFeeRule loadLateFeeRule(UUID ruleId) {
		return lateFeeRuleRepository.findByIdAndDeletedFalse(ruleId)
				.orElseThrow(() -> new ResourceNotFoundException("Late fee rule", ruleId));
	}

	private StudentFeeAssignment loadAssignment(UUID assignmentId) {
		return assignmentRepository.findDetailedByIdAndDeletedFalse(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student fee assignment", assignmentId));
	}

	private StudentFeeAssignmentResponse processPaymentAction(
			UUID paymentId,
			PaymentActionRequest request,
			FeePaymentStatus targetStatus,
			String auditAction) {
		FeePayment payment = feePaymentRepository.findDetailedByIdAndDeletedFalse(paymentId)
				.orElseThrow(() -> new ResourceNotFoundException("Fee payment", paymentId));
		if (!payment.isCompleted()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Only completed payments can be reversed, voided, or refunded.");
		}
		StudentFeeAssignment assignment = payment.getAssignment();
		StudentFeeAssignmentResponse oldValue = feeMapper.toAssignmentResponse(assignment);
		String reason = request == null ? null : request.reason();
		assignment.reversePayment(payment, targetStatus, reason);
		StudentFeeAssignmentResponse response = feeMapper.toAssignmentResponse(assignment);
		audit(
				"FeePayment",
				paymentId,
				auditAction,
				oldValue,
				Map.of(
						"paymentId", paymentId,
						"targetStatus", targetStatus,
						"assignment", response,
						"processedBy", request == null ? currentActor() : firstText(request.processedBy(), currentActor()),
						"actionDate", request == null || request.actionDate() == null ? LocalDate.now() : request.actionDate()));
		return response;
	}

	private void validateFeeStructureRequest(FeeStructureRequest request) {
		BigDecimal itemTotal = request.items().stream()
				.map(item -> money(item.amount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal installmentTotal = request.installments().stream()
				.map(installment -> money(installment.amount()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (itemTotal.compareTo(installmentTotal) != 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Fee structure item total must equal installment total.");
		}
		long uniqueSequences = request.installments().stream()
				.map(installment -> installment.sequenceNo())
				.distinct()
				.count();
		if (uniqueSequences != request.installments().size()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Installment sequence numbers must be unique.");
		}
	}

	private void applyFeeStructureLines(FeeStructure structure, FeeStructureRequest request) {
		request.items().forEach(item -> {
			FeeCategory category = loadCategory(item.categoryId());
			if (!category.isActive()) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Fee category is inactive: " + category.getCode());
			}
			structure.addItem(category, item.amount(), item.mandatory(), item.sortOrder());
		});
		request.installments().forEach(installment -> structure.addInstallment(
				installment.sequenceNo(),
				installment.title(),
				installment.dueDate(),
				installment.amount()));
	}

	private BigDecimal calculateDiscountAmount(DiscountCalculationType calculationType, BigDecimal value, BigDecimal basis) {
		BigDecimal normalizedValue = money(value);
		if (calculationType == DiscountCalculationType.PERCENTAGE) {
			if (normalizedValue.compareTo(BigDecimal.valueOf(100)) > 0) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Percentage discount cannot exceed 100.");
			}
			return basis.multiply(normalizedValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		}
		return normalizedValue;
	}

	private List<LateFeeRule> matchingLateFeeRules(StudentFeeAssignment assignment) {
		return lateFeeRuleRepository.findByAcademicYearIgnoreCaseAndActiveTrueAndDeletedFalse(assignment.getAcademicYear()).stream()
				.filter(rule -> rule.appliesTo(assignment))
				.sorted(Comparator.comparing((LateFeeRule rule) -> StringUtils.hasText(rule.getClassName())).reversed()
						.thenComparing(rule -> StringUtils.hasText(rule.getSectionName()), Comparator.reverseOrder()))
				.toList();
	}

	private void validatePaymentReference(PaymentMode mode, String referenceNumber) {
		String normalizedReference = blankToNull(referenceNumber);
		if (mode != PaymentMode.CASH && normalizedReference == null) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Reference number is required for non-cash payments.");
		}
		if (normalizedReference != null
				&& feePaymentRepository.existsByPaymentModeAndReferenceNumberAndDeletedFalse(mode, normalizedReference)) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Reference number already exists for this payment mode.");
		}
	}

	private String generateReceiptNumber() {
		String datePart = LocalDate.now().format(RECEIPT_DATE_FORMAT);
		String receiptNumber;
		do {
			String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
			receiptNumber = "RCPT-" + datePart + "-" + suffix;
		}
		while (feeReceiptRepository.existsByReceiptNumberAndDeletedFalse(receiptNumber));
		return receiptNumber;
	}

	private LocalDate defaultDate(LocalDate value) {
		return value == null ? LocalDate.now() : value;
	}

	private BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
	}

	private boolean mandatory(FeeCategoryRequest request) {
		return request.mandatory() == null || request.mandatory();
	}

	private String blankToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String firstText(String primary, String fallback) {
		return StringUtils.hasText(primary) ? primary.trim() : fallback;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue));
	}

	private record ResolvedFeeStructureClass(AcademicYear academicYear, ClassEntity classEntity) {
	}
}
