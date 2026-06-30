package com.school.erp.modules.hostel.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.hostel.domain.HostelBed;

public interface HostelBedRepository extends BaseRepository<HostelBed, UUID> {

	List<HostelBed> findByRoomIdAndActiveTrueAndDeletedFalseOrderByBedNumberAsc(UUID roomId);

	Optional<HostelBed> findByRoomIdAndBedNumberIgnoreCaseAndDeletedFalse(UUID roomId, String bedNumber);
}
