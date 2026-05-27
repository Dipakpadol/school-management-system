package com.school.erp.modules.students.infrastructure;

import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.students.domain.StudentClassAssignment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentClassAssignmentRepository extends BaseRepository<StudentClassAssignment, UUID> {

	@Query("""
			select count(assignment) > 0
			from StudentClassAssignment assignment
			where assignment.deleted = false
			  and assignment.active = true
			  and assignment.academicYearEntity.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.sectionEntity.id = :sectionId
			  and lower(assignment.rollNumber) = lower(:rollNumber)
			  and (:excludedAssignmentId is null or assignment.id <> :excludedAssignmentId)
			""")
	boolean existsActiveRollNumber(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("rollNumber") String rollNumber,
			@Param("excludedAssignmentId") UUID excludedAssignmentId);
}
