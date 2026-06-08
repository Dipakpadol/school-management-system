package com.school.erp.modules.exams.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.exams.domain.ExamSchedule;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamScheduleRepository extends BaseRepository<ExamSchedule, UUID> {

	@EntityGraph(attributePaths = { "academicYear", "classEntity", "section", "examType", "subjects", "subjects.subject", "subject" })
	@Query("""
			select schedule
			from ExamSchedule schedule
			where schedule.id = :id
			  and schedule.deleted = false
			""")
	Optional<ExamSchedule> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "academicYear", "classEntity", "section", "examType", "subjects", "subjects.subject", "subject" })
	@Query("""
			select distinct schedule
			from ExamSchedule schedule
			where schedule.deleted = false
			  and (:academicYearId is null or schedule.academicYear.id = :academicYearId)
			  and (:classId is null or schedule.classEntity.id = :classId)
			  and (:sectionId is null or schedule.section.id = :sectionId)
			order by schedule.examName asc, schedule.createdAt asc
			""")
	List<ExamSchedule> search(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId);

	@EntityGraph(attributePaths = { "academicYear", "classEntity", "section", "examType", "subjects", "subjects.subject", "subject" })
	@Query("""
			select distinct schedule
			from ExamSchedule schedule
			where schedule.deleted = false
			  and schedule.academicYear.id = :academicYearId
			  and schedule.classEntity.id = :classId
			  and schedule.section.id = :sectionId
			  and (:examTypeId is null or schedule.examType.id = :examTypeId)
			  and (:examScheduleId is null or schedule.id = :examScheduleId)
			order by schedule.examType.displayOrder asc, schedule.examName asc, schedule.createdAt asc
			""")
	List<ExamSchedule> findForStudentProfile(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("examTypeId") UUID examTypeId,
			@Param("examScheduleId") UUID examScheduleId);

	@Query("""
			select count(schedule) > 0
			from ExamSchedule schedule
			where schedule.deleted = false
			  and schedule.academicYear.id = :academicYearId
			  and schedule.classEntity.id = :classId
			  and schedule.section.id = :sectionId
			  and schedule.examType.id = :examTypeId
			  and schedule.subject.id = :subjectId
			  and (:excludedId is null or schedule.id <> :excludedId)
			""")
	boolean existsDuplicate(
			@Param("academicYearId") UUID academicYearId,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("examTypeId") UUID examTypeId,
			@Param("subjectId") UUID subjectId,
			@Param("excludedId") UUID excludedId);

	long countByExamTypeIdAndDeletedFalse(UUID examTypeId);
}
