package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.TransportVehicle;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportVehicleRepository extends BaseRepository<TransportVehicle, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "academicYear", "driver" })
	@Query("select vehicle from TransportVehicle vehicle where vehicle.id = :id and vehicle.deleted = false")
	Optional<TransportVehicle> lockByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "academicYear", "driver" })
	List<TransportVehicle> findByAcademicYearIdAndDeletedFalseOrderByVehicleNumberAsc(UUID academicYearId);

	@EntityGraph(attributePaths = { "academicYear", "driver" })
	Optional<TransportVehicle> findByVehicleNumberIgnoreCaseAndAcademicYearIdAndDeletedFalse(
			String vehicleNumber,
			UUID academicYearId);
}
