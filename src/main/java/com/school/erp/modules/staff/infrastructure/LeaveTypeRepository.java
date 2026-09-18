package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.LeaveType;

public interface LeaveTypeRepository extends BaseRepository<LeaveType, UUID> {

	Optional<LeaveType> findByNameIgnoreCaseAndDeletedFalse(String name);

	boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedFalse(String name);

	List<LeaveType> findAllByDeletedFalseOrderByNameAsc();
}
