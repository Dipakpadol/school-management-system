package com.school.erp.modules.exams.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.exams.domain.ExamScheduleSubject;

import org.springframework.data.jpa.repository.EntityGraph;

public interface ExamScheduleSubjectRepository extends BaseRepository<ExamScheduleSubject, UUID> {

	@EntityGraph(attributePaths = { "subject", "examSchedule", "examSchedule.academicYear", "examSchedule.classEntity", "examSchedule.section" })
	List<ExamScheduleSubject> findByExamScheduleIdAndDeletedFalse(UUID examScheduleId);

	@EntityGraph(attributePaths = { "subject", "examSchedule", "examSchedule.academicYear", "examSchedule.classEntity", "examSchedule.section" })
	Optional<ExamScheduleSubject> findByExamScheduleIdAndSubjectIdAndDeletedFalse(UUID examScheduleId, UUID subjectId);

	boolean existsByExamScheduleIdAndSubjectIdAndDeletedFalse(UUID examScheduleId, UUID subjectId);
}
