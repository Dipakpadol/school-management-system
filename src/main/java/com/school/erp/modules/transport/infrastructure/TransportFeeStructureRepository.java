package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.transport.domain.TransportFeeStructure;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportFeeStructureRepository extends BaseRepository<TransportFeeStructure, UUID> {

	@EntityGraph(attributePaths = { "backingFeeStructure", "route", "pickupPoint" })
	@Query("""
			select structure
			from TransportFeeStructure structure
			where structure.deleted = false
			  and structure.academicYear.id = :academicYearId
			  and structure.route.id = :routeId
			  and structure.status = :status
			  and (
			    structure.pickupPoint is null
			    or structure.pickupPoint.id = :pickupPointId
			  )
			order by case when structure.pickupPoint is null then 1 else 0 end,
			         structure.createdAt asc
			""")
	List<TransportFeeStructure> findApplicable(
			@Param("academicYearId") UUID academicYearId,
			@Param("routeId") UUID routeId,
			@Param("pickupPointId") UUID pickupPointId,
			@Param("status") FeeStructureStatus status);
}
