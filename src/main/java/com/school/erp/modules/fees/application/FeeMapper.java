package com.school.erp.modules.fees.application;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

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
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

	public FeeStructureResponse toStructureResponse(FeeStructure structure) {
		return new FeeStructureResponse(
				structure.getId(),
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
		return new StudentFeeAssignmentResponse(
				assignment.getId(),
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				assignment.getFeeStructure().getId(),
				assignment.getFeeStructure().getName(),
				assignment.getAcademicYear(),
				assignment.getClassName(),
				assignment.getSectionName(),
				assignment.getAssignedDate(),
				assignment.getGrossAmount(),
				assignment.getDiscountAmount(),
				assignment.getLateFeeAmount(),
				assignment.getPaidAmount(),
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
				.filter(installment -> installment.getDueDate().isBefore(asOf))
				.sorted(Comparator.comparing(StudentFeeInstallment::getDueDate))
				.toList();
		Student student = assignment.getStudent();
		return new FeeDefaulterResponse(
				assignment.getId(),
				student.getId(),
				student.getAdmissionNumber(),
				student.getDisplayName(),
				assignment.getAcademicYear(),
				assignment.getClassName(),
				assignment.getSectionName(),
				assignment.getBalanceAmount(),
				overdueInstallments.isEmpty() ? null : overdueInstallments.getFirst().getDueDate(),
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
}
