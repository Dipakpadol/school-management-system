package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.Subject;

public interface SubjectRepository extends BaseRepository<Subject, UUID> {

	List<Subject> findAllByDeletedFalseAndActiveTrueOrderByNameAsc();

	Optional<Subject> findByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<Subject> findByNameIgnoreCaseAndDeletedFalse(String name);
}
