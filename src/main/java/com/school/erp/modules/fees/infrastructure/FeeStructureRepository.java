package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeScope;
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
			  and structure.status = :status
			  and lower(structure.academicYear) = lower(:academicYear)
			  and lower(structure.className) = lower(:className)
			  and lower(structure.name) = lower(:structureName)
			  and (
			    (:sectionName is null and structure.sectionName is null)
			    or lower(structure.sectionName) = lower(:sectionName)
			  )
			""")
	boolean existsActiveStructureForClass(
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("structureName") String structureName,
			@Param("status") com.school.erp.modules.fees.domain.FeeStructureStatus status);

	@Query("""
			select count(structure) > 0
			from FeeStructure structure
			where structure.deleted = false
			  and structure.id <> :structureId
			  and structure.status = :status
			  and lower(structure.academicYear) = lower(:academicYear)
			  and lower(structure.className) = lower(:className)
			  and lower(structure.name) = lower(:structureName)
			  and (
			    (:sectionName is null and structure.sectionName is null)
			    or lower(structure.sectionName) = lower(:sectionName)
			  )
			""")
	boolean existsStructureForClassExcludingId(
			@Param("structureId") UUID structureId,
			@Param("academicYear") String academicYear,
			@Param("className") String className,
			@Param("sectionName") String sectionName,
			@Param("structureName") String structureName,
			@Param("status") com.school.erp.modules.fees.domain.FeeStructureStatus status);

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

	List<FeeStructure> findByClassEntityIdAndStatusAndDeletedFalseOrderByCreatedAtAsc(
			UUID classId,
			com.school.erp.modules.fees.domain.FeeStructureStatus status);

	List<FeeStructure> findByAcademicYearEntityIdAndClassEntityIdAndFeeScopeAndStatusAndDeletedFalseOrderByCreatedAtAsc(
			UUID academicYearId,
			UUID classId,
			FeeScope feeScope,
			com.school.erp.modules.fees.domain.FeeStructureStatus status);

	Page<FeeStructure> findByAcademicYearEntityIdAndClassEntityIdAndDeletedFalse(
			UUID academicYearId,
			UUID classId,
			Pageable pageable);

	Page<FeeStructure> findByAcademicYearEntityIdAndClassEntityIdAndStatusAndDeletedFalse(
			UUID academicYearId,
			UUID classId,
			com.school.erp.modules.fees.domain.FeeStructureStatus status,
			Pageable pageable);

	@Query("""
			select structure
			from FeeStructure structure
			where structure.deleted = false
			  and (:status is null or structure.status = :status)
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
			@Param("status") com.school.erp.modules.fees.domain.FeeStructureStatus status,
			Pageable pageable);

	Page<FeeStructure> findByAcademicYearEntityIdAndDeletedFalse(UUID academicYearId, Pageable pageable);

	Page<FeeStructure> findByAcademicYearEntityIdAndStatusAndDeletedFalse(
			UUID academicYearId,
			com.school.erp.modules.fees.domain.FeeStructureStatus status,
			Pageable pageable);

	Page<FeeStructure> findByClassEntityIdAndDeletedFalse(UUID classId, Pageable pageable);

	Page<FeeStructure> findByClassEntityIdAndStatusAndDeletedFalse(
			UUID classId,
			com.school.erp.modules.fees.domain.FeeStructureStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = { "items", "items.category", "installments" })
	@Query("select distinct structure from FeeStructure structure where structure.id = :id and structure.deleted = false")
	Optional<FeeStructure> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);
}
