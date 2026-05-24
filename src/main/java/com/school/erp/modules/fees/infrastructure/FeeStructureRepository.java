package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeStructure;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	Optional<FeeStructure> findByAcademicYearIgnoreCaseAndClassNameIgnoreCaseAndSectionNameIgnoreCaseAndDeletedFalse(
			String academicYear,
			String className,
			String sectionName);

	@EntityGraph(attributePaths = { "items", "items.category", "installments" })
	@Query("select distinct structure from FeeStructure structure where structure.id = :id and structure.deleted = false")
	Optional<FeeStructure> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);
}
