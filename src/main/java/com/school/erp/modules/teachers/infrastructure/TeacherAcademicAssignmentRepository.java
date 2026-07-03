package com.school.erp.modules.teachers.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.Teacher;
import com.school.erp.modules.teachers.domain.TeacherAcademicAssignment;
import com.school.erp.modules.teachers.domain.TeacherAssignmentStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherAcademicAssignmentRepository extends BaseRepository<TeacherAcademicAssignment, UUID> {

	@EntityGraph(attributePaths = { "teacher", "academicYear", "classEntity", "section", "subject" })
	List<TeacherAcademicAssignment> findByTeacherIdAndDeletedFalseOrderByCreatedAtDesc(UUID teacherId);

	@EntityGraph(attributePaths = { "teacher", "academicYear", "classEntity", "section", "subject" })
	List<TeacherAcademicAssignment> findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(
			UUID teacherId,
			UUID academicYearId);

	long countByTeacherIdAndAcademicYearIdAndDeletedFalse(UUID teacherId, UUID academicYearId);

	@Query("""
			select distinct assignment.teacher
			from TeacherAcademicAssignment assignment
			where assignment.deleted = false
			  and assignment.academicYear.id = :academicYearId
			  and assignment.status = :status
			  and assignment.teacher.deleted = false
			  and assignment.teacher.active = true
			order by assignment.teacher.firstName asc, assignment.teacher.lastName asc
			""")
	List<Teacher> findActiveTeachersByAcademicYearId(
			@Param("academicYearId") UUID academicYearId,
			@Param("status") TeacherAssignmentStatus status);
}
