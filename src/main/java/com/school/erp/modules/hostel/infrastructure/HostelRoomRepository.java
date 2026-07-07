package com.school.erp.modules.hostel.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.hostel.domain.HostelRoom;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostelRoomRepository extends BaseRepository<HostelRoom, UUID> {

	@EntityGraph(attributePaths = { "hostel", "beds" })
	@Query("select distinct room from HostelRoom room where room.id = :id and room.deleted = false")
	Optional<HostelRoom> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);

	List<HostelRoom> findByActiveTrueAndDeletedFalseOrderByHostelNameAscRoomNumberAsc();

	@EntityGraph(attributePaths = { "hostel", "beds" })
	List<HostelRoom> findByDeletedFalseOrderByHostelNameAscRoomNumberAsc();

	List<HostelRoom> findByHostelIdAndActiveTrueAndDeletedFalseOrderByRoomNumberAsc(UUID hostelId);

	Optional<HostelRoom> findByHostelIdAndRoomNumberIgnoreCaseAndDeletedFalse(UUID hostelId, String roomNumber);

	@Query("""
			select room
			from HostelRoom room
			where room.deleted = false
			  and room.active = true
			  and lower(room.roomNumber) = lower(:roomNumber)
			  and (:hostelId is null or room.hostel.id = :hostelId)
			order by room.hostel.name asc
			""")
	List<HostelRoom> findActiveByRoomNumber(
			@Param("hostelId") UUID hostelId,
			@Param("roomNumber") String roomNumber);
}
