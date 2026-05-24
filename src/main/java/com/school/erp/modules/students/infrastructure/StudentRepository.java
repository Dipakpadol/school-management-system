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
}
