package com.school.erp.modules.exams.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.exams.domain.ExamMark;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamMarkRepository extends BaseRepository<ExamMark, UUID> {

	@EntityGraph(attributePaths = { "student", "examSchedule", "subject" })
	@Query("""
			select mark
			from ExamMark mark
			where mark.deleted = false
			  and mark.academicYear.id = :academicYearId
			  and mark.classEntity.id = :classId
			  and mark.section.id = :sectionId
			  and mark.examSchedule.id = :examScheduleId
			  and mark.subject.id = :subjectId
			order by mark.student.firstName asc, mark.student.lastName asc
			""")
	List<ExamMark> findMarks(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("examScheduleId") UUID examScheduleId,
			@Param("subjectId") UUID subjectId);

	@EntityGraph(attributePaths = { "student", "examSchedule", "subject" })
	@Query("""
			select mark
			from ExamMark mark
			where mark.deleted = false
			  and mark.academicYear.id = :academicYearId
			  and mark.classEntity.id = :classId
			  and mark.section.id = :sectionId
			  and (:examTypeId is null or mark.examSchedule.examType.id = :examTypeId)
			order by mark.student.firstName asc, mark.subject.name asc
			""")
	List<ExamMark> findResultMarks(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("examTypeId") UUID examTypeId);

	@EntityGraph(attributePaths = { "student", "examSchedule", "subject", "classEntity", "section" })
	@Query("""
			select mark
			from ExamMark mark
			where mark.deleted = false
			  and mark.student.id = :studentId
			  and mark.academicYear.id = :academicYearId
			order by mark.examSchedule.examDate asc, mark.subject.name asc
			""")
	List<ExamMark> findStudentMarks(
			@Param("studentId") UUID studentId,
			@Param("academicYearId") UUID academicYearId);

	long countByExamScheduleIdAndDeletedFalse(UUID examScheduleId);
}
