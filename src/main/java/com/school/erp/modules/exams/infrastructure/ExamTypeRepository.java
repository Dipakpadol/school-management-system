package com.school.erp.modules.exams.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.exams.domain.ExamType;

public interface ExamTypeRepository extends BaseRepository<ExamType, UUID> {

	Optional<ExamType> findByCodeIgnoreCaseAndDeletedFalse(String code);

	List<ExamType> findAllByDeletedFalseOrderByDisplayOrderAscNameAsc();
}
