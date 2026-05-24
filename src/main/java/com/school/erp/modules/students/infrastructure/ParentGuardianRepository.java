package com.school.erp.modules.students.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.students.domain.ParentGuardian;

public interface ParentGuardianRepository extends BaseRepository<ParentGuardian, UUID> {

	Optional<ParentGuardian> findByEmailIgnoreCaseAndDeletedFalse(String email);
}
