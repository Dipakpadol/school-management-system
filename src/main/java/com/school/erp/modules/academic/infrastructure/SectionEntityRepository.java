package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.SectionEntity;

public interface SectionEntityRepository extends BaseRepository<SectionEntity, UUID> {

	List<SectionEntity> findByClassEntityIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(UUID classId);

	Optional<SectionEntity> findByClassEntityIdAndNameIgnoreCaseAndDeletedFalse(UUID classId, String name);

	Optional<SectionEntity> findByClassEntityIdAndCodeIgnoreCaseAndDeletedFalse(UUID classId, String code);

	long countByClassEntityIdAndDeletedFalse(UUID classId);
}
