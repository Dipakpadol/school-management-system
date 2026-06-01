package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.ClassEntity;

public interface ClassEntityRepository extends BaseRepository<ClassEntity, UUID> {

	List<ClassEntity> findByAcademicYearIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(UUID academicYearId);

	Optional<ClassEntity> findByAcademicYearIdAndNameIgnoreCaseAndDeletedFalse(UUID academicYearId, String name);

	Optional<ClassEntity> findByAcademicYearIdAndCodeIgnoreCaseAndDeletedFalse(UUID academicYearId, String code);

	long countByAcademicYearIdAndDeletedFalse(UUID academicYearId);
}
