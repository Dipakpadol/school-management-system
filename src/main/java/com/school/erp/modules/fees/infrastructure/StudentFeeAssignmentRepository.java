package com.school.erp.modules.fees.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeInstallmentStatus;
import com.school.erp.modules.fees.domain.StudentFeeAssignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentFeeAssignmentRepository
		extends BaseRepository<StudentFeeAssignment, UUID>, JpaSpecificationExecutor<StudentFeeAssignment> {

	boolean existsByStudentIdAndFeeStructureIdAndDeletedFalse(UUID studentId, UUID feeStructureId);

	boolean existsByFeeStructureIdAndDeletedFalse(UUID feeStructureId);

	Optional<StudentFeeAssignment> findByStudentIdAndFeeStructureIdAndDeletedFalse(UUID studentId, UUID feeStructureId);

	List<StudentFeeAssignment> findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(UUID studentId);

	List<StudentFeeAssignment> findByClassEntityIdAndDeletedFalseOrderByAssignedDateDesc(UUID classId);

	@EntityGraph(attributePaths = {
			"student",
			"feeStructure",
			"installments",
			"discounts",
			"discounts.installment",
			"payments",
			"payments.receipt",
			"payments.allocations",
			"payments.allocations.installment"
	})
	@Query("select distinct assignment from StudentFeeAssignment assignment where assignment.id = :id and assignment.deleted = false")
	Optional<StudentFeeAssignment> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "student", "installments" })
	@Query(
			value = """
					select distinct assignment
					from StudentFeeAssignment assignment
					join assignment.installments installment
					join assignment.student student
					where assignment.deleted = false
					  and installment.deleted = false
					  and installment.status not in (:paidStatus, :cancelledStatus)
					  and installment.dueDate < :asOf
					  and assignment.balanceAmount >= :minimumBalance
					  and (:academicYear is null or lower(assignment.academicYear) = lower(:academicYear))
					  and (:className is null or lower(assignment.className) = lower(:className))
					  and (:sectionName is null or lower(assignment.sectionName) = lower(:sectionName))
					""",
			countQuery = """
					select count(distinct assignment)
					from StudentFeeAssignment assignment
					join assignment.installments installment
					where assignment.deleted = false
					  and installment.deleted = false
					  and installment.status not in (:paidStatus, :cancelledStatus)
					  and installment.dueDate < :asOf
					  and assignment.balanceAmount >= :minimumBalance
					  and (:academicYear is null or lower(assignment.academicYear) = lower(:academicYear))
					  and (:className is null or lower(assignment.className) = lower(:className))
					  and (:sectionName is null or lower(assignment.sectionName) = lower(:sectionName))
					""")
	Page<StudentFeeAssignment> findDefaulters(
			@Param("asOf") LocalDate asOf,
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("minimumBalance") BigDecimal minimumBalance,
			@Param("paidStatus") FeeInstallmentStatus paidStatus,
			@Param("cancelledStatus") FeeInstallmentStatus cancelledStatus,
			Pageable pageable);

	@Query("""
			select
			  count(assignment) as assignments,
			  coalesce(sum(assignment.grossAmount), 0) as grossAmount,
			  coalesce(sum(assignment.discountAmount), 0) as discountAmount,
			  coalesce(sum(assignment.lateFeeAmount), 0) as lateFeeAmount,
			  coalesce(sum(assignment.paidAmount), 0) as paidAmount,
			  coalesce(sum(assignment.balanceAmount), 0) as balanceAmount
			from StudentFeeAssignment assignment
			where assignment.deleted = false
			""")
	FeeReportTotals summarizeAll();

	@Query("""
			select
			  count(assignment) as assignments,
			  coalesce(sum(assignment.grossAmount), 0) as grossAmount,
			  coalesce(sum(assignment.discountAmount), 0) as discountAmount,
			  coalesce(sum(assignment.lateFeeAmount), 0) as lateFeeAmount,
			  coalesce(sum(assignment.paidAmount), 0) as paidAmount,
			  coalesce(sum(assignment.balanceAmount), 0) as balanceAmount
			from StudentFeeAssignment assignment
			where assignment.deleted = false
			  and (:academicYear is null or lower(assignment.academicYear) = lower(:academicYear))
			  and (:className is null or lower(assignment.className) = lower(:className))
			  and (:sectionName is null or lower(assignment.sectionName) = lower(:sectionName))
			  and (:status is null or assignment.status = :status)
			""")
	FeeReportTotals summarize(
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("status") FeeAssignmentStatus status);
}
