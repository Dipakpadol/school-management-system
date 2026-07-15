package com.school.erp.modules.documents.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.documents.domain.GeneratedDocument;
import com.school.erp.modules.documents.domain.GeneratedDocumentStatus;
import com.school.erp.modules.documents.domain.GeneratedDocumentType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeneratedDocumentRepository extends BaseRepository<GeneratedDocument, UUID> {

	boolean existsByStudentIdAndDocumentTypeAndStatusAndDeletedFalse(
			UUID studentId,
			GeneratedDocumentType documentType,
			GeneratedDocumentStatus status);

	@EntityGraph(attributePaths = { "student", "academicYear", "reprintOf" })
	@Query("""
			select document
			from GeneratedDocument document
			where document.deleted = false
			  and document.id = :id
			""")
	Optional<GeneratedDocument> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "student", "academicYear", "reprintOf" })
	@Query("""
			select document
			from GeneratedDocument document
			where document.deleted = false
			  and document.student.id = :studentId
			order by document.issueDate desc, document.createdAt desc
			""")
	List<GeneratedDocument> findByStudentIdOrderByIssueDateDesc(@Param("studentId") UUID studentId);
}
