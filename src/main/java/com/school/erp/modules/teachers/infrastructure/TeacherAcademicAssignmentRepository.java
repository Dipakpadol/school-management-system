package com.school.erp.modules.teachers.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.teachers.domain.TeacherAcademicAssignment;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TeacherAcademicAssignmentRepository extends BaseRepository<TeacherAcademicAssignment, UUID> {

	@EntityGraph(attributePaths = { "teacher", "academicYear", "classEntity", "section", "subject" })
	List<TeacherAcademicAssignment> findByTeacherIdAndDeletedFalseOrderByCreatedAtDesc(UUID teacherId);

	@EntityGraph(attributePaths = { "teacher", "academicYear", "classEntity", "section", "subject" })
	List<TeacherAcademicAssignment> findByTeacherIdAndAcademicYearIdAndDeletedFalseOrderByCreatedAtDesc(
			UUID teacherId,
			UUID academicYearId);

	long countByTeacherIdAndAcademicYearIdAndDeletedFalse(UUID teacherId, UUID academicYearId);
}
