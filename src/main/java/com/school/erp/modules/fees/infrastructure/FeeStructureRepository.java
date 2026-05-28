package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeStructure;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeeStructureRepository extends BaseRepository<FeeStructure, UUID>, JpaSpecificationExecutor<FeeStructure> {

	@Query("""
			select count(structure) > 0
			from FeeStructure structure
			where structure.deleted = false
			  and lower(structure.academicYear) = lower(:academicYear)
			  and lower(structure.className) = lower(:className)
			  and (
			    (:sectionName is null and structure.sectionName is null)
			    or lower(structure.sectionName) = lower(:sectionName)
			  )
			""")
	boolean existsActiveStructureForClass(
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName);

	@Query("""
			select count(structure) > 0
			from FeeStructure structure
			where structure.deleted = false
			  and structure.id <> :structureId
			  and lower(structure.academicYear) = lower(:academicYear)
			  and lower(structure.className) = lower(:className)
			  and (
			    (:sectionName is null and structure.sectionName is null)
			    or lower(structure.sectionName) = lower(:sectionName)
			  )
			""")
	boolean existsStructureForClassExcludingId(
			@Param("structureId") UUID structureId,
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName);

	@Query("""
			select count(structure) > 0
			from FeeStructure structure
			join structure.items item
			where structure.deleted = false
			  and item.deleted = false
			  and item.category.id = :categoryId
			""")
	boolean existsByCategoryId(@Param("categoryId") UUID categoryId);

	Optional<FeeStructure> findByAcademicYearIgnoreCaseAndClassNameIgnoreCaseAndSectionNameIgnoreCaseAndDeletedFalse(
			String academicYear,
			String className,
			String sectionName);

	Optional<FeeStructure> findFirstByClassEntityIdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
			UUID classId,
			com.school.erp.modules.fees.domain.FeeStructureStatus status);

	Page<FeeStructure> findByAcademicYearEntityIdAndClassEntityIdAndDeletedFalse(
			UUID academicYearId,
			UUID classId,
			Pageable pageable);

	@Query("""
			select structure
			from FeeStructure structure
			where structure.deleted = false
			  and (
			    (structure.academicYearEntity is not null and structure.academicYearEntity.id = :academicYearId)
			    or (structure.academicYearEntity is null and lower(structure.academicYear) = lower(:academicYearName))
			    or (structure.academicYearEntity is null and lower(structure.academicYear) = lower(:academicYearCode))
			  )
			  and (
			    (structure.classEntity is not null and structure.classEntity.id = :classId)
			    or (structure.classEntity is null and lower(structure.className) = lower(:className))
			    or (structure.classEntity is null and lower(structure.className) = lower(:classCode))
			  )
			""")
	Page<FeeStructure> findByAcademicYearAndClassMappingOrLegacy(
			@Param("academicYearId") UUID academicYearId,
			@Param("academicYearName") String academicYearName,
			@Param("academicYearCode") String academicYearCode,
			@Param("classId") UUID classId,
			@Param("className") String className,
			@Param("classCode") String classCode,
			Pageable pageable);

	Page<FeeStructure> findByAcademicYearEntityIdAndDeletedFalse(UUID academicYearId, Pageable pageable);

	Page<FeeStructure> findByClassEntityIdAndDeletedFalse(UUID classId, Pageable pageable);

	@EntityGraph(attributePaths = { "items", "items.category", "installments" })
	@Query("select distinct structure from FeeStructure structure where structure.id = :id and structure.deleted = false")
	Optional<FeeStructure> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);
}
