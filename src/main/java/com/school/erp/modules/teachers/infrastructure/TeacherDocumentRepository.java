package com.school.erp.modules.teachers.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.teachers.domain.TeacherDocument;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TeacherDocumentRepository extends BaseRepository<TeacherDocument, UUID> {

	@EntityGraph(attributePaths = { "teacher" })
	List<TeacherDocument> findByTeacherIdAndDeletedFalseOrderByUploadedAtDesc(UUID teacherId);
}
