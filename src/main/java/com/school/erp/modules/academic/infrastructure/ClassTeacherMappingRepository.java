package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.ClassTeacherMapping;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ClassTeacherMappingRepository extends BaseRepository<ClassTeacherMapping, UUID> {

	@EntityGraph(attributePaths = { "teacher", "classEntity", "section" })
	Optional<ClassTeacherMapping> findByClassEntityIdAndSectionIdAndActiveTrueAndDeletedFalse(UUID classId, UUID sectionId);

	@EntityGraph(attributePaths = { "teacher", "classEntity", "classEntity.academicYear", "section" })
	List<ClassTeacherMapping> findByTeacherIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(UUID teacherId);

	@EntityGraph(attributePaths = { "teacher", "classEntity", "classEntity.academicYear", "section" })
	List<ClassTeacherMapping> findByTeacherIdAndClassEntityAcademicYearIdAndActiveTrueAndDeletedFalseOrderByEffectiveFromDesc(
			UUID teacherId,
			UUID academicYearId);
}
