package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.TransportVehicle;

import org.springframework.data.jpa.repository.EntityGraph;

public interface TransportVehicleRepository extends BaseRepository<TransportVehicle, UUID> {

	@EntityGraph(attributePaths = { "academicYear", "driver" })
	List<TransportVehicle> findByAcademicYearIdAndDeletedFalseOrderByVehicleNumberAsc(UUID academicYearId);

	@EntityGraph(attributePaths = { "academicYear", "driver" })
	Optional<TransportVehicle> findByVehicleNumberIgnoreCaseAndAcademicYearIdAndDeletedFalse(
			String vehicleNumber,
			UUID academicYearId);
}
