package com.school.erp.modules.hostel.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.hostel.domain.Hostel;

public interface HostelRepository extends BaseRepository<Hostel, UUID> {

	Optional<Hostel> findByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<Hostel> findByNameIgnoreCaseAndDeletedFalse(String name);

	List<Hostel> findAllByDeletedFalseOrderByNameAsc();

	List<Hostel> findAllByActiveTrueAndDeletedFalseOrderByNameAsc();
}
