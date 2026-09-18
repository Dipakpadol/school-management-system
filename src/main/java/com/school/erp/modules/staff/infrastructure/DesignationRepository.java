package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.Designation;

public interface DesignationRepository extends BaseRepository<Designation, UUID> {

	Optional<Designation> findByNameIgnoreCaseAndDeletedFalse(String name);

	boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedFalse(String name);

	List<Designation> findAllByDeletedFalseOrderByNameAsc();
}
