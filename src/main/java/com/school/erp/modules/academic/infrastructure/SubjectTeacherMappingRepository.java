package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.SubjectTeacherMapping;

import org.springframework.data.jpa.repository.EntityGraph;

public interface SubjectTeacherMappingRepository extends BaseRepository<SubjectTeacherMapping, UUID> {

	@EntityGraph(attributePaths = { "teacher", "subject", "classEntity", "section" })
	List<SubjectTeacherMapping> findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalseOrderBySubjectNameAscTeacherFirstNameAsc(
			UUID classId,
			UUID sectionId);

	Optional<SubjectTeacherMapping> findByClassEntityIdAndSectionIdAndSubjectIdAndTeacherIdAndActiveTrueAndDeletedFalse(
			UUID classId,
			UUID sectionId,
			UUID subjectId,
			UUID teacherId);

	@EntityGraph(attributePaths = { "teacher", "subject", "classEntity", "classEntity.academicYear", "section" })
	List<SubjectTeacherMapping> findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(UUID teacherId);

	@EntityGraph(attributePaths = { "teacher", "subject", "classEntity", "classEntity.academicYear", "section" })
	List<SubjectTeacherMapping> findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
			UUID teacherId,
			UUID academicYearId);
}
