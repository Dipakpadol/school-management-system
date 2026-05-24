package com.school.erp.modules.fees.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.LateFeeRule;

public interface LateFeeRuleRepository extends BaseRepository<LateFeeRule, UUID> {

	List<LateFeeRule> findByAcademicYearIgnoreCaseAndActiveTrueAndDeletedFalse(String academicYear);
}
