package com.school.erp.modules.students.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.students.domain.StudentClassAssignment;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
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

	@Query("""
			select count(assignment)
			from StudentClassAssignment assignment
			where assignment.deleted = false
			  and assignment.active = true
			  and assignment.sectionEntity.id = :sectionId
			""")
	long countActiveBySectionId(@Param("sectionId") UUID sectionId);

	@Query("""
			select count(distinct student.id)
			from StudentClassAssignment assignment
			join assignment.student student
			where assignment.deleted = false
			  and assignment.active = true
			  and student.deleted = false
			  and (:academicYearId is null or assignment.academicYearEntity.id = :academicYearId)
			  and (:classId is null or assignment.classEntity.id = :classId)
			  and (:sectionId is null or assignment.sectionEntity.id = :sectionId)
			""")
	long countActiveStudents(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId);

	@EntityGraph(attributePaths = { "student" })
	@Query("""
			select assignment
			from StudentClassAssignment assignment
			join assignment.student student
			where assignment.deleted = false
			  and assignment.active = true
			  and student.deleted = false
			  and assignment.academicYearEntity.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.sectionEntity.id = :sectionId
			order by assignment.rollNumber asc, student.firstName asc, student.lastName asc, student.admissionNumber asc
			""")
	List<StudentClassAssignment> findActiveByHierarchy(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId);

	@Query("""
			select count(assignment) > 0
			from StudentClassAssignment assignment
			join assignment.student student
			where assignment.deleted = false
			  and assignment.active = true
			  and student.deleted = false
			  and student.id = :studentId
			  and assignment.academicYearEntity.id = :academicYearId
			  and assignment.classEntity.id = :classId
			  and assignment.sectionEntity.id = :sectionId
			""")
	boolean existsActiveStudentInHierarchy(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId);
}
