package com.school.erp.modules.fees.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.students.domain.Student;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_fee_assignments")
@SQLRestriction("deleted = false")
public class StudentFeeAssignment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fee_structure_id", nullable = false)
	private FeeStructure feeStructure;

	@Column(name = "academic_year", nullable = false, length = 20)
	private String academicYear;

	@Column(name = "class_name", nullable = false, length = 80)
	private String className;

	@Column(name = "section_name", length = 80)
	private String sectionName;

	@Column(name = "assigned_date", nullable = false)
	private LocalDate assignedDate;

	@Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal grossAmount = BigDecimal.ZERO;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount = BigDecimal.ZERO;

	@Column(name = "late_fee_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal lateFeeAmount = BigDecimal.ZERO;

	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal balanceAmount = BigDecimal.ZERO;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FeeAssignmentStatus status = FeeAssignmentStatus.PENDING;

	@Column(length = 500)
	private String notes;

	@OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
	private Set<StudentFeeInstallment> installments = new LinkedHashSet<>();

	@OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
	private Set<FeeDiscount> discounts = new LinkedHashSet<>();

	@OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL)
	private Set<FeePayment> payments = new LinkedHashSet<>();

	public StudentFeeAssignment(Student student, FeeStructure feeStructure, LocalDate assignedDate, String notes) {
		this.student = student;
		this.feeStructure = feeStructure;
		this.academicYear = feeStructure.getAcademicYear();
		this.className = feeStructure.getClassName();
		this.sectionName = feeStructure.getSectionName();
		this.assignedDate = assignedDate;
		this.notes = notes;
		this.grossAmount = feeStructure.getTotalAmount();
		this.balanceAmount = feeStructure.getTotalAmount();
	}

	public StudentFeeInstallment addInstallment(int sequenceNo, String title, LocalDate dueDate, BigDecimal amount) {
		StudentFeeInstallment installment = new StudentFeeInstallment(this, sequenceNo, title, dueDate, amount);
		installments.add(installment);
		recalculate();
		return installment;
	}

	public Optional<StudentFeeInstallment> findInstallment(java.util.UUID installmentId) {
		return installments.stream()
				.filter(installment -> installment.getId() != null && installment.getId().equals(installmentId))
				.findFirst();
	}

	public void applyDiscount(FeeDiscount discount) {
		discounts.add(discount);
		BigDecimal remaining = discount.getAmount();
		if (discount.getInstallment() != null) {
			discount.getInstallment().applyDiscount(remaining);
			recalculate();
			return;
		}
		for (StudentFeeInstallment installment : orderedInstallments()) {
			if (remaining.signum() == 0) {
				break;
			}
			BigDecimal allocation = remaining.min(installment.discountableBalance());
			if (allocation.signum() > 0) {
				installment.applyDiscount(allocation);
				remaining = remaining.subtract(allocation);
			}
		}
		recalculate();
	}

	public void assessLateFees(Iterable<LateFeeRule> rules, LocalDate asOf) {
		for (StudentFeeInstallment installment : orderedInstallments()) {
			if (installment.isSettledOrCancelled()) {
				continue;
			}
			BigDecimal lateFee = BigDecimal.ZERO;
			for (LateFeeRule rule : rules) {
				if (rule.appliesTo(this)) {
					lateFee = rule.calculate(installment, asOf);
					break;
				}
			}
			installment.assessLateFee(lateFee);
		}
		recalculate();
	}

	public void collectPayment(FeePayment payment) {
		BigDecimal remaining = payment.getAmount();
		for (StudentFeeInstallment installment : orderedInstallments()) {
			if (remaining.signum() == 0) {
				break;
			}
			BigDecimal allocation = remaining.min(installment.getBalanceAmount());
			if (allocation.signum() > 0) {
				installment.applyPayment(allocation);
				payment.addAllocation(installment, allocation);
				remaining = remaining.subtract(allocation);
			}
		}
		payments.add(payment);
		recalculate();
	}

	public void reversePayment(FeePayment payment, FeePaymentStatus targetStatus, String reason) {
		payment.getAllocations().forEach(allocation -> allocation.getInstallment().reversePayment(allocation.getAmount()));
		if (targetStatus == FeePaymentStatus.REVERSED) {
			payment.reverse(reason);
		}
		else if (targetStatus == FeePaymentStatus.VOIDED) {
			payment.voidPayment(reason);
		}
		else if (targetStatus == FeePaymentStatus.REFUNDED) {
			payment.refund(reason);
		}
		payment.getReceipt().cancel();
		recalculate();
	}

	public Set<StudentFeeInstallment> orderedInstallments() {
		return installments.stream()
				.sorted(Comparator.comparing(StudentFeeInstallment::getDueDate)
						.thenComparingInt(StudentFeeInstallment::getSequenceNo))
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	public void cancel() {
		status = FeeAssignmentStatus.CANCELLED;
		installments.forEach(StudentFeeInstallment::cancel);
	}

	public void recalculate() {
		grossAmount = installments.stream()
				.map(StudentFeeInstallment::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		discountAmount = installments.stream()
				.map(StudentFeeInstallment::getDiscountAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		lateFeeAmount = installments.stream()
				.map(StudentFeeInstallment::getLateFeeAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		paidAmount = installments.stream()
				.map(StudentFeeInstallment::getPaidAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		balanceAmount = installments.stream()
				.map(StudentFeeInstallment::getBalanceAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		updateStatus();
	}

	private void updateStatus() {
		if (status == FeeAssignmentStatus.CANCELLED) {
			return;
		}
		if (balanceAmount.signum() == 0) {
			status = FeeAssignmentStatus.PAID;
		}
		else if (installments.stream().anyMatch(StudentFeeInstallment::isOverdue)) {
			status = FeeAssignmentStatus.OVERDUE;
		}
		else if (paidAmount.signum() > 0) {
			status = FeeAssignmentStatus.PARTIALLY_PAID;
		}
		else {
			status = FeeAssignmentStatus.PENDING;
		}
	}

	static BigDecimal money(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
	}
}
