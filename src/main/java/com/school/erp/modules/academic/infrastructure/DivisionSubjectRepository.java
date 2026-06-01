package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.DivisionSubject;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DivisionSubjectRepository extends BaseRepository<DivisionSubject, UUID> {

	boolean existsBySectionIdAndSubjectIdAndDeletedFalse(UUID sectionId, UUID subjectId);

	@EntityGraph(attributePaths = { "section", "subject", "teacher" })
	@Query("""
			select mapping from DivisionSubject mapping
			where mapping.section.id = :sectionId and mapping.deleted = false
			order by mapping.subject.name asc
			""")
	List<DivisionSubject> findBySectionIdAndDeletedFalseOrderBySubjectNameAsc(@Param("sectionId") UUID sectionId);

	@EntityGraph(attributePaths = { "section", "subject", "teacher" })
	@Query("select mapping from DivisionSubject mapping where mapping.id = :id and mapping.deleted = false")
	Optional<DivisionSubject> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);
}
