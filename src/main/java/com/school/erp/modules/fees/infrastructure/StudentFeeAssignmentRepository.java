package com.school.erp.modules.fees.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeScope;
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

	@Query("""
			select count(assignment) > 0
			from StudentFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.student.id = :studentId
			  and assignment.feeScope = :feeScope
			  and assignment.status <> :cancelledStatus
			  and assignment.academicYearEntity.id = :academicYearId
			  and assignment.hostel.id = :hostelId
			  and (
			    assignment.hostelRoom.id = :roomId
			    or (assignment.hostelRoom is null and :roomType is not null and lower(assignment.roomType) = lower(:roomType))
			    or (assignment.hostelRoom is null and assignment.roomType is null)
			  )
			""")
	boolean existsActiveHostelAssignmentForAllocation(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId,
			@Param("hostelId") UUID hostelId,
			@Param("roomId") UUID roomId,
			@Param("roomType") String roomType,
			@Param("feeScope") FeeScope feeScope,
			@Param("cancelledStatus") FeeAssignmentStatus cancelledStatus);

	@Query("""
			select count(assignment) > 0
			from StudentFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.student.id = :studentId
			  and assignment.feeScope = :feeScope
			  and assignment.status <> :cancelledStatus
			  and assignment.academicYearEntity.id = :academicYearId
			  and assignment.transportRoute.id = :routeId
			  and (
			    assignment.transportPickupPoint.id = :pickupPointId
			    or assignment.transportPickupPoint is null
			  )
			""")
	boolean existsActiveTransportAssignmentForRoute(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId,
			@Param("routeId") UUID routeId,
			@Param("pickupPointId") UUID pickupPointId,
			@Param("feeScope") FeeScope feeScope,
			@Param("cancelledStatus") FeeAssignmentStatus cancelledStatus);

	Optional<StudentFeeAssignment> findByStudentIdAndFeeStructureIdAndDeletedFalse(UUID studentId, UUID feeStructureId);

	List<StudentFeeAssignment> findByStudentIdAndDeletedFalseOrderByAssignedDateDesc(UUID studentId);

	List<StudentFeeAssignment> findByClassEntityIdAndDeletedFalseOrderByAssignedDateDesc(UUID classId);

	@EntityGraph(attributePaths = {
			"student",
			"student.parents",
			"student.parents.parent",
			"installments"
	})
	@Query("""
			select distinct assignment
			from StudentFeeAssignment assignment
			join assignment.installments installment
			where assignment.deleted = false
			  and installment.deleted = false
			  and assignment.status in :statuses
			  and assignment.balanceAmount > 0
			  and installment.balanceAmount > 0
			  and installment.dueDate <= :asOf
			order by assignment.assignedDate asc
			""")
	List<StudentFeeAssignment> findDueReminderCandidates(
			@Param("asOf") LocalDate asOf,
			@Param("statuses") List<FeeAssignmentStatus> statuses);

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
					  and (:academicYearId is null or assignment.academicYearEntity.id = :academicYearId)
					  and (:classId is null or assignment.classEntity.id = :classId)
					  and (:academicYear is null or lower(assignment.academicYear) = lower(:academicYear))
					  and (:className is null or lower(assignment.className) = lower(:className))
					  and (:sectionName is null or lower(assignment.sectionName) = lower(:sectionName))
					  and (:studentName is null or lower(student.admissionNumber) like lower(concat('%', :studentName, '%'))
					    or lower(student.firstName) like lower(concat('%', :studentName, '%'))
					    or lower(student.middleName) like lower(concat('%', :studentName, '%'))
					    or lower(student.lastName) like lower(concat('%', :studentName, '%')))
					""",
			countQuery = """
					select count(distinct assignment)
					from StudentFeeAssignment assignment
					join assignment.installments installment
					join assignment.student student
					where assignment.deleted = false
					  and installment.deleted = false
					  and installment.status not in (:paidStatus, :cancelledStatus)
					  and installment.dueDate < :asOf
					  and assignment.balanceAmount >= :minimumBalance
					  and (:academicYearId is null or assignment.academicYearEntity.id = :academicYearId)
					  and (:classId is null or assignment.classEntity.id = :classId)
					  and (:academicYear is null or lower(assignment.academicYear) = lower(:academicYear))
					  and (:className is null or lower(assignment.className) = lower(:className))
					  and (:sectionName is null or lower(assignment.sectionName) = lower(:sectionName))
					  and (:studentName is null or lower(student.admissionNumber) like lower(concat('%', :studentName, '%'))
					    or lower(student.firstName) like lower(concat('%', :studentName, '%'))
					    or lower(student.middleName) like lower(concat('%', :studentName, '%'))
					    or lower(student.lastName) like lower(concat('%', :studentName, '%')))
					""")
	Page<StudentFeeAssignment> findDefaulters(
			@Param("asOf") LocalDate asOf,
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("studentName") String studentName,
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
