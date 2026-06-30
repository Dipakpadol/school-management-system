package com.school.erp.modules.hostel.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.hostel.domain.HostelFeeStructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostelFeeStructureRepository extends BaseRepository<HostelFeeStructure, UUID> {

	@EntityGraph(attributePaths = { "academicYear", "hostel", "room", "feeCategory", "backingFeeStructure" })
	Optional<HostelFeeStructure> findDetailedByIdAndDeletedFalse(UUID id);

	@EntityGraph(attributePaths = { "academicYear", "hostel", "room", "feeCategory", "backingFeeStructure" })
	@Query("""
			select structure
			from HostelFeeStructure structure
			where structure.deleted = false
			  and (:academicYearId is null or structure.academicYear.id = :academicYearId)
			  and (:hostelId is null or structure.hostel.id = :hostelId)
			  and (:roomType is null or lower(structure.roomType) = lower(:roomType))
			order by structure.academicYear.startDate desc, structure.hostel.name asc, structure.roomType asc
			""")
	List<HostelFeeStructure> search(
			@Param("academicYearId") UUID academicYearId,
			@Param("hostelId") UUID hostelId,
			@Param("roomType") String roomType);

	@EntityGraph(attributePaths = { "academicYear", "hostel", "room", "feeCategory", "backingFeeStructure" })
	@Query("""
			select structure
			from HostelFeeStructure structure
			where structure.deleted = false
			  and (:academicYearId is null or structure.academicYear.id = :academicYearId)
			  and (:hostelId is null or structure.hostel.id = :hostelId)
			  and (:roomType is null or lower(structure.roomType) = lower(:roomType))
			  and (:status is null or structure.status = :status)
			""")
	Page<HostelFeeStructure> search(
			@Param("academicYearId") UUID academicYearId,
			@Param("hostelId") UUID hostelId,
			@Param("roomType") String roomType,
			@Param("status") FeeStructureStatus status,
			Pageable pageable);

	@Query("""
			select structure
			from HostelFeeStructure structure
			where structure.deleted = false
			  and structure.status = :status
			  and structure.academicYear.id = :academicYearId
			  and structure.hostel.id = :hostelId
			  and (
			    structure.room.id = :roomId
			    or (structure.room is null and structure.roomType is not null and lower(structure.roomType) = lower(:roomType))
			    or (structure.room is null and structure.roomType is null)
			  )
			order by
			  case when structure.room.id = :roomId then 0 when structure.roomType is not null then 1 else 2 end,
			  structure.createdAt asc
			""")
	List<HostelFeeStructure> findApplicable(
			@Param("academicYearId") UUID academicYearId,
			@Param("hostelId") UUID hostelId,
			@Param("roomId") UUID roomId,
			@Param("roomType") String roomType,
			@Param("status") FeeStructureStatus status);

	boolean existsByBackingFeeStructureIdAndDeletedFalse(UUID backingFeeStructureId);

	@Query("""
			select count(structure) > 0
			from HostelFeeStructure structure
			where structure.deleted = false
			  and (:excludedId is null or structure.id <> :excludedId)
			  and structure.academicYear.id = :academicYearId
			  and structure.hostel.id = :hostelId
			  and structure.feeCategory.id = :feeCategoryId
			  and (
			    (:roomId is null and structure.room is null)
			    or (:roomId is not null and structure.room.id = :roomId)
			  )
			  and (
			    (:roomType is null and structure.roomType is null)
			    or (:roomType is not null and lower(structure.roomType) = lower(:roomType))
			  )
			""")
	boolean existsDuplicateScope(
			@Param("academicYearId") UUID academicYearId,
			@Param("hostelId") UUID hostelId,
			@Param("roomId") UUID roomId,
			@Param("roomType") String roomType,
			@Param("feeCategoryId") UUID feeCategoryId,
			@Param("excludedId") UUID excludedId);
}
