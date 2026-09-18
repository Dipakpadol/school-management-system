package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.Department;

public interface DepartmentRepository extends BaseRepository<Department, UUID> {

	Optional<Department> findByNameIgnoreCaseAndDeletedFalse(String name);

	boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedFalse(String name);

	List<Department> findAllByDeletedFalseOrderByNameAsc();
}
