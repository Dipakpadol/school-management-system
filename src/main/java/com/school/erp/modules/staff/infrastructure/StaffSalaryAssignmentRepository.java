package com.school.erp.modules.staff.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.StaffSalaryAssignment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffSalaryAssignmentRepository extends BaseRepository<StaffSalaryAssignment, UUID> {

	@EntityGraph(attributePaths = { "staff", "salaryStructure" })
	List<StaffSalaryAssignment> findByStaffIdAndDeletedFalseOrderByEffectiveFromDesc(UUID staffId);

	@EntityGraph(attributePaths = { "staff", "salaryStructure" })
	@Query("""
			select assignment
			from StaffSalaryAssignment assignment
			where assignment.deleted = false
			  and assignment.staff.id = :staffId
			  and assignment.effectiveFrom <= :asOfDate
			  and (assignment.effectiveTo is null or assignment.effectiveTo >= :asOfDate)
			order by assignment.effectiveFrom desc
			""")
	List<StaffSalaryAssignment> findEffectiveAssignments(
			@Param("staffId") UUID staffId,
			@Param("asOfDate") LocalDate asOfDate);
}
