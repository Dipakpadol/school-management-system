package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeCategory;

public interface FeeCategoryRepository extends BaseRepository<FeeCategory, UUID> {

	boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<FeeCategory> findByCodeIgnoreCaseAndDeletedFalse(String code);
}
