package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.transport.domain.TransportFeeStructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransportFeeStructureRepository extends BaseRepository<TransportFeeStructure, UUID> {

	@EntityGraph(attributePaths = { "academicYear", "route", "pickupPoint", "feeCategory", "backingFeeStructure" })
	Optional<TransportFeeStructure> findDetailedByIdAndDeletedFalse(UUID id);

	@EntityGraph(attributePaths = { "academicYear", "route", "pickupPoint", "feeCategory", "backingFeeStructure" })
	@Query("""
			select structure
			from TransportFeeStructure structure
			where structure.deleted = false
			  and (:academicYearId is null or structure.academicYear.id = :academicYearId)
			  and (:routeId is null or structure.route.id = :routeId)
			  and (:pickupPointId is null or structure.pickupPoint.id = :pickupPointId)
			  and (:status is null or structure.status = :status)
			""")
	Page<TransportFeeStructure> search(
			@Param("academicYearId") UUID academicYearId,
			@Param("routeId") UUID routeId,
			@Param("pickupPointId") UUID pickupPointId,
			@Param("status") FeeStructureStatus status,
			Pageable pageable);

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

	boolean existsByBackingFeeStructureIdAndDeletedFalse(UUID backingFeeStructureId);

	@Query("""
			select count(structure) > 0
			from TransportFeeStructure structure
			where structure.deleted = false
			  and (:excludedId is null or structure.id <> :excludedId)
			  and structure.academicYear.id = :academicYearId
			  and structure.route.id = :routeId
			  and structure.feeCategory.id = :feeCategoryId
			  and (
			    (:pickupPointId is null and structure.pickupPoint is null)
			    or (:pickupPointId is not null and structure.pickupPoint.id = :pickupPointId)
			  )
			""")
	boolean existsDuplicateScope(
			@Param("academicYearId") UUID academicYearId,
			@Param("routeId") UUID routeId,
			@Param("pickupPointId") UUID pickupPointId,
			@Param("feeCategoryId") UUID feeCategoryId,
			@Param("excludedId") UUID excludedId);
}
