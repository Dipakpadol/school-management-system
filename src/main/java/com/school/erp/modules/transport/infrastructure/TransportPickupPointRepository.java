package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.TransportPickupPoint;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TransportPickupPointRepository extends BaseRepository<TransportPickupPoint, UUID> {

	@EntityGraph(attributePaths = { "route", "route.vehicle" })
	List<TransportPickupPoint> findByRouteIdAndDeletedFalseOrderBySequenceOrderAscPointNameAsc(UUID routeId);

	Optional<TransportPickupPoint> findByRouteIdAndPointNameIgnoreCaseAndDeletedFalse(UUID routeId, String pointName);
}
