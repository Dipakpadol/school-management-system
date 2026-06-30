package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.TransportRoute;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TransportRouteRepository extends BaseRepository<TransportRoute, UUID> {

	@EntityGraph(attributePaths = { "academicYear", "vehicle", "vehicle.driver" })
	List<TransportRoute> findByAcademicYearIdAndDeletedFalseOrderByRouteNameAsc(UUID academicYearId);

	@EntityGraph(attributePaths = { "academicYear", "vehicle", "vehicle.driver" })
	List<TransportRoute> findByVehicleIdAndAcademicYearIdAndDeletedFalseOrderByRouteNameAsc(UUID vehicleId, UUID academicYearId);

	Optional<TransportRoute> findByRouteCodeIgnoreCaseAndAcademicYearIdAndDeletedFalse(String routeCode, UUID academicYearId);

	Optional<TransportRoute> findByRouteNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(String routeName, UUID academicYearId);
}
