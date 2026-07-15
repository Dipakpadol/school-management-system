package com.school.erp.modules.fees.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.ClassFeeAssignment;
import com.school.erp.modules.fees.domain.ClassFeeAssignmentStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassFeeAssignmentRepository extends BaseRepository<ClassFeeAssignment, UUID> {

	@Query("""
			select count(assignment) > 0
			from ClassFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.academicYear.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.feeStructure.id = :feeStructureId
			  and assignment.status = :status
			""")
	boolean existsActive(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("feeStructureId") UUID feeStructureId,
			@Param("status") ClassFeeAssignmentStatus status);

	@EntityGraph(attributePaths = {
			"academicYear",
			"classEntity",
			"feeStructure",
			"feeStructure.items",
			"feeStructure.items.category",
			"feeStructure.installments"
	})
	@Query("""
			select assignment
			from ClassFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.academicYear.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.feeStructure.id = :feeStructureId
			  and assignment.status = :status
			""")
	Optional<ClassFeeAssignment> findActive(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("feeStructureId") UUID feeStructureId,
			@Param("status") ClassFeeAssignmentStatus status);

	@EntityGraph(attributePaths = {
			"academicYear",
			"classEntity",
			"feeStructure",
			"feeStructure.items",
			"feeStructure.items.category",
			"feeStructure.installments"
	})
	@Query("""
			select distinct assignment
			from ClassFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.academicYear.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.status = :status
			order by assignment.assignedDate asc, assignment.createdAt asc
			""")
	List<ClassFeeAssignment> findActiveByAcademicYearAndClass(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("status") ClassFeeAssignmentStatus status);

	@EntityGraph(attributePaths = {
			"academicYear",
			"classEntity",
			"feeStructure",
			"feeStructure.items",
			"feeStructure.items.category",
			"feeStructure.installments"
	})
	@Query("""
			select distinct assignment
			from ClassFeeAssignment assignment
			where assignment.deleted = false
			  and assignment.classEntity.id = :classId
			  and (:academicYearId is null or assignment.academicYear.id = :academicYearId)
			order by assignment.assignedDate desc, assignment.createdAt desc
			""")
	List<ClassFeeAssignment> findByClassAndOptionalAcademicYear(
			@Param("classId") UUID classId,
			@Param("academicYearId") UUID academicYearId);
}
