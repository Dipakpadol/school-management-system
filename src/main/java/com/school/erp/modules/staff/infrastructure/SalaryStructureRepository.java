package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.SalaryStructure;

public interface SalaryStructureRepository extends BaseRepository<SalaryStructure, UUID> {

	Optional<SalaryStructure> findByCodeIgnoreCaseAndDeletedFalse(String code);

	boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);

	List<SalaryStructure> findAllByDeletedFalseOrderByNameAsc();
}
