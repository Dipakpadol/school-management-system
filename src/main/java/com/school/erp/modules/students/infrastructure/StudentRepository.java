package com.school.erp.modules.students.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.students.domain.Student;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends BaseRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

	boolean existsByAdmissionNumberIgnoreCaseAndDeletedFalse(String admissionNumber);

	Optional<Student> findByAdmissionNumberIgnoreCaseAndDeletedFalse(String admissionNumber);

	@Query("""
			select count(assignment) > 0
			from StudentClassAssignment assignment
			where assignment.deleted = false
			  and lower(assignment.academicYear) = lower(:academicYear)
			  and lower(assignment.className) = lower(:className)
			  and lower(assignment.sectionName) = lower(:sectionName)
			  and lower(assignment.rollNumber) = lower(:rollNumber)
			""")
	boolean existsRollNumberInClassSectionYear(
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("rollNumber") String rollNumber);

	@EntityGraph(attributePaths = { "parents", "parents.parent", "documents", "classAssignments" })
	@Query("select distinct student from Student student where student.id = :id and student.deleted = false")
	Optional<Student> findProfileByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "classAssignments" })
	@Query("""
			select distinct student
			from Student student
			where student.deleted = false
			  and month(student.dateOfBirth) = :month
			  and day(student.dateOfBirth) = :day
			order by student.firstName asc, student.lastName asc
			""")
	List<Student> findBirthdaysByMonthAndDay(
			@Param("month") int month,
			@Param("day") int day,
			Pageable pageable);

	@EntityGraph(attributePaths = { "classAssignments" })
	@Query("""
			select distinct student
			from Student student
			join student.classAssignments assignment
			where student.deleted = false
			  and assignment.deleted = false
			  and assignment.active = true
			  and assignment.classEntity.id = :classId
			order by student.firstName asc, student.lastName asc, student.admissionNumber asc
			""")
	List<Student> findActiveStudentsByClassId(@Param("classId") UUID classId);
}
