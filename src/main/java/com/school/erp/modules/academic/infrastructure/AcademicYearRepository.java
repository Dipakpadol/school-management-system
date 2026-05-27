package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.AcademicYear;

public interface AcademicYearRepository extends BaseRepository<AcademicYear, UUID> {

	List<AcademicYear> findAllByDeletedFalseOrderByStartDateDescNameAsc();

	Optional<AcademicYear> findByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<AcademicYear> findByNameIgnoreCaseAndDeletedFalse(String name);
}
