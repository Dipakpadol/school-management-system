package com.school.erp.modules.fees.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.api.dto.FeeCategoryResponse;
import com.school.erp.modules.fees.api.dto.FeeDefaulterResponse;
import com.school.erp.modules.fees.api.dto.FeeDiscountResponse;
import com.school.erp.modules.fees.api.dto.FeePaymentAllocationResponse;
import com.school.erp.modules.fees.api.dto.FeePaymentResponse;
import com.school.erp.modules.fees.api.dto.FeeReceiptResponse;
import com.school.erp.modules.fees.api.dto.FeeReportSummaryResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureInstallmentResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureItemResponse;
import com.school.erp.modules.fees.api.dto.FeeStructureResponse;
import com.school.erp.modules.fees.api.dto.LateFeeRuleResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.api.dto.StudentFeeInstallmentResponse;
import com.school.erp.modules.fees.domain.FeeCategory;
import com.school.erp.modules.fees.domain.FeeDiscount;
import com.school.erp.modules.fees.domain.FeePayment;
import com.school.erp.modules.fees.domain.FeePaymentAllocation;
import com.school.erp.modules.fees.domain.FeeReceipt;
import com.school.erp.modules.fees.domain.FeeStructure;
import com.school.erp.modules.fees.domain.FeeStructureInstallment;
import com.school.erp.modules.fees.domain.FeeStructureItem;
import com.school.erp.modules.fees.domain.LateFeeRule;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;
import com.school.erp.modules.fees.domain.StudentFeeInstallment;
import com.school.erp.modules.fees.infrastructure.FeeReportTotals;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.domain.StudentClassAssignment;
import com.school.erp.modules.students.domain.StudentParent;

import org.springframework.stereotype.Component;

@Component
public class FeeMapper {

	public FeeCategoryResponse toCategoryResponse(FeeCategory category) {
		return new FeeCategoryResponse(
				category.getId(),
				category.getCode(),
				category.getName(),
				category.getDescription(),
				category.isActive(),
				category.getSortOrder(),
				category.isMandatory(),
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

	public FeeStructureResponse toStructureResponse(FeeStructure structure) {
		return new FeeStructureResponse(
				structure.getId(),
				structure.getAcademicYearEntity() == null ? null : structure.getAcademicYearEntity().getId(),
				structure.getClassEntity() == null ? null : structure.getClassEntity().getId(),
				structure.getFeeScope(),
				structure.getHostel() == null ? null : structure.getHostel().getId(),
				structure.getHostel() == null ? null : structure.getHostel().getName(),
				structure.getHostelRoom() == null ? null : structure.getHostelRoom().getId(),
				structure.getHostelRoom() == null ? null : structure.getHostelRoom().getRoomNumber(),
				structure.getRoomType(),
				structure.getTransportRoute() == null ? null : structure.getTransportRoute().getId(),
				structure.getTransportRoute() == null ? null : structure.getTransportRoute().getRouteName(),
				structure.getTransportPickupPoint() == null ? null : structure.getTransportPickupPoint().getId(),
				structure.getTransportPickupPoint() == null ? null : structure.getTransportPickupPoint().getPointName(),
				structure.getAcademicYear(),
				structure.getClassName(),
				structure.getSectionName(),
				structure.getName(),
				structure.getDescription(),
				structure.getStatus(),
				structure.getTotalAmount(),
				structure.getItems().stream()
						.sorted(Comparator.comparingInt(FeeStructureItem::getSortOrder))
						.map(this::toStructureItemResponse)
						.toList(),
				structure.getInstallments().stream()
						.sorted(Comparator.comparingInt(FeeStructureInstallment::getSequenceNo))
						.map(this::toStructureInstallmentResponse)
						.toList(),
				structure.getCreatedAt(),
				structure.getUpdatedAt());
	}

	public StudentFeeAssignmentResponse toAssignmentResponse(StudentFeeAssignment assignment) {
		Student student = assignment.getStudent();
		UUID sectionId = sectionId(assignment);
		return new StudentFeeAssignmentResponse(
				assignment.getId(),
				assignment.getId(),
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				assignment.getFeeStructure().getId(),
				assignment.getFeeStructure().getName(),
				feeCategoryName(assignment.getFeeStructure()),
				assignment.getAcademicYearEntity() == null ? null : assignment.getAcademicYearEntity().getId(),
				assignment.getAcademicYear(),
				assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId(),
				sectionId,
				assignment.getFeeScope(),
				assignment.getSourceType(),
				assignment.getSourceReferenceId(),
				assignment.getHostel() == null ? null : assignment.getHostel().getId(),
				assignment.getHostel() == null ? null : assignment.getHostel().getName(),
				assignment.getHostelRoom() == null ? null : assignment.getHostelRoom().getId(),
				assignment.getHostelRoom() == null ? null : assignment.getHostelRoom().getRoomNumber(),
				assignment.getRoomType(),
				assignment.getTransportRoute() == null ? null : assignment.getTransportRoute().getId(),
				assignment.getTransportRoute() == null ? null : assignment.getTransportRoute().getRouteName(),
				assignment.getTransportPickupPoint() == null ? null : assignment.getTransportPickupPoint().getId(),
				assignment.getTransportPickupPoint() == null ? null : assignment.getTransportPickupPoint().getPointName(),
				assignment.getAcademicYear(),
				assignment.getClassName(),
				assignment.getSectionName(),
				assignment.getAssignedDate(),
				assignment.getGrossAmount(),
				assignment.getDiscountAmount(),
				assignment.getLateFeeAmount(),
				assignment.getPayableAmount(),
				assignment.getPaidAmount(),
				assignment.getBalanceAmount(),
				assignment.getGrossAmount(),
				assignment.getBalanceAmount(),
				assignment.getStatus(),
				assignment.getNotes(),
				assignment.getInstallments().stream()
						.sorted(Comparator.comparing(StudentFeeInstallment::getDueDate)
								.thenComparingInt(StudentFeeInstallment::getSequenceNo))
						.map(this::toInstallmentResponse)
						.toList(),
				assignment.getDiscounts().stream()
						.sorted(Comparator.comparing(FeeDiscount::getApprovedAt))
						.map(this::toDiscountResponse)
						.toList(),
				assignment.getPayments().stream()
						.sorted(Comparator.comparing(FeePayment::getPaymentDate))
						.map(this::toPaymentResponse)
						.toList(),
				assignment.getCreatedAt(),
				assignment.getUpdatedAt());
	}

	public FeeReceiptResponse toReceiptResponse(FeeReceipt receipt) {
		Student student = receipt.getStudent();
		return new FeeReceiptResponse(
				receipt.getId(),
				receipt.getReceiptNumber(),
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				receipt.getAssignment().getId(),
				receipt.getReceiptDate(),
				receipt.getTotalAmount(),
				receipt.getPayerName(),
				receipt.getPaymentMode(),
				receipt.getStatus());
	}

	public FeeDefaulterResponse toDefaulterResponse(StudentFeeAssignment assignment, LocalDate asOf) {
		List<StudentFeeInstallment> overdueInstallments = assignment.getInstallments().stream()
				.filter(installment -> installment.getBalanceAmount().signum() > 0)
				.filter(installment -> !installment.getDueDate().isAfter(asOf))
				.sorted(Comparator.comparing(StudentFeeInstallment::getDueDate))
				.toList();
		Student student = assignment.getStudent();
		LocalDate dueDate = overdueInstallments.isEmpty() ? null : overdueInstallments.getFirst().getDueDate();
		StudentParent primaryParent = primaryParent(student);
		return new FeeDefaulterResponse(
				assignment.getId(),
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				assignment.getAcademicYear(),
				assignment.getClassName(),
				assignment.getSectionName(),
				assignment.getSourceType(),
				assignment.getGrossAmount(),
				assignment.getPaidAmount(),
				assignment.getBalanceAmount(),
				dueDate,
				dueDate == null ? 0 : Math.max(0, ChronoUnit.DAYS.between(dueDate, asOf)),
				primaryParent == null ? student.getPhoneNumber() : primaryParent.getParent().getPhoneNumber(),
				primaryParent == null ? null : primaryParent.getParent().getDisplayName(),
				assignment.getBalanceAmount(),
				dueDate,
				overdueInstallments.size());
	}

	public FeeReportSummaryResponse toReportSummary(FeeReportTotals totals) {
		return new FeeReportSummaryResponse(
				totals.getAssignments(),
				totals.getGrossAmount(),
				totals.getDiscountAmount(),
				totals.getLateFeeAmount(),
				totals.getPaidAmount(),
				totals.getBalanceAmount());
	}

	public LateFeeRuleResponse toLateFeeRuleResponse(LateFeeRule rule) {
		return new LateFeeRuleResponse(
				rule.getId(),
				rule.getName(),
				rule.getAcademicYear(),
				rule.getClassName(),
				rule.getSectionName(),
				rule.getGraceDays(),
				rule.getCalculationType(),
				rule.getAmount(),
				rule.getMaxAmount(),
				rule.isActive(),
				rule.getCreatedAt(),
				rule.getUpdatedAt());
	}

	private FeeStructureItemResponse toStructureItemResponse(FeeStructureItem item) {
		FeeCategory category = item.getCategory();
		return new FeeStructureItemResponse(
				item.getId(),
				category.getId(),
				category.getCode(),
				category.getName(),
				item.getAmount(),
				item.isMandatory(),
				item.getSortOrder());
	}

	private FeeStructureInstallmentResponse toStructureInstallmentResponse(FeeStructureInstallment installment) {
		return new FeeStructureInstallmentResponse(
				installment.getId(),
				installment.getSequenceNo(),
				installment.getTitle(),
				installment.getDueDate(),
				installment.getAmount());
	}

	private StudentFeeInstallmentResponse toInstallmentResponse(StudentFeeInstallment installment) {
		return new StudentFeeInstallmentResponse(
				installment.getId(),
				installment.getSequenceNo(),
				installment.getTitle(),
				installment.getDueDate(),
				installment.getAmount(),
				installment.getDiscountAmount(),
				installment.getLateFeeAmount(),
				installment.getPayableAmount(),
				installment.getPaidAmount(),
				installment.getBalanceAmount(),
				installment.getStatus());
	}

	private FeeDiscountResponse toDiscountResponse(FeeDiscount discount) {
		return new FeeDiscountResponse(
				discount.getId(),
				discount.getInstallment() == null ? null : discount.getInstallment().getId(),
				discount.getDiscountType(),
				discount.getCalculationType(),
				discount.getValue(),
				discount.getAmount(),
				discount.getReason(),
				discount.getApprovedBy(),
				discount.getApprovedAt());
	}

	private FeePaymentResponse toPaymentResponse(FeePayment payment) {
		return new FeePaymentResponse(
				payment.getId(),
				payment.getReceipt().getReceiptNumber(),
				payment.getAmount(),
				payment.getPaymentDate(),
				payment.getPaymentMode(),
				payment.getReferenceNumber(),
				payment.getPayerName(),
				payment.getCollectedBy(),
				payment.getRemarks(),
				payment.getStatus(),
				payment.getAllocations().stream()
						.map(this::toPaymentAllocationResponse)
						.toList());
	}

	private FeePaymentAllocationResponse toPaymentAllocationResponse(FeePaymentAllocation allocation) {
		return new FeePaymentAllocationResponse(
				allocation.getInstallment().getId(),
				allocation.getInstallment().getTitle(),
				allocation.getAmount());
	}

	private String feeCategoryName(FeeStructure structure) {
		return structure.getItems().stream()
				.filter(item -> !item.isDeleted())
				.sorted(Comparator.comparingInt(FeeStructureItem::getSortOrder))
				.map(item -> item.getCategory().getName())
				.distinct()
				.reduce((left, right) -> left + ", " + right)
				.orElse(null);
	}

	private UUID sectionId(StudentFeeAssignment assignment) {
		UUID assignmentAcademicYearId = assignment.getAcademicYearEntity() == null
				? null
				: assignment.getAcademicYearEntity().getId();
		UUID assignmentClassId = assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId();
		return assignment.getStudent().getClassAssignments().stream()
				.filter(classAssignment -> !classAssignment.isDeleted())
				.filter(StudentClassAssignment::isActive)
				.filter(classAssignment -> assignmentAcademicYearId == null
						|| (classAssignment.getAcademicYearEntity() != null
								&& assignmentAcademicYearId.equals(classAssignment.getAcademicYearEntity().getId())))
				.filter(classAssignment -> assignmentClassId == null
						|| (classAssignment.getClassEntity() != null
								&& assignmentClassId.equals(classAssignment.getClassEntity().getId())))
				.map(StudentClassAssignment::getSectionEntity)
				.filter(java.util.Objects::nonNull)
				.map(section -> section.getId())
				.findFirst()
				.orElse(null);
	}

	private StudentParent primaryParent(Student student) {
		return student.getParents().stream()
				.filter(parent -> !parent.isDeleted())
				.sorted(Comparator.comparing(StudentParent::isPrimaryContact).reversed())
				.findFirst()
				.orElse(null);
	}
}
