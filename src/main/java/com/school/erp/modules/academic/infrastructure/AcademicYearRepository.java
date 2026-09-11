package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.AcademicYear;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicYearRepository extends BaseRepository<AcademicYear, UUID> {

	List<AcademicYear> findAllByDeletedFalseOrderByStartDateDescNameAsc();

	Optional<AcademicYear> findByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<AcademicYear> findByNameIgnoreCaseAndDeletedFalse(String name);

	Optional<AcademicYear> findFirstByCurrentYearTrueAndDeletedFalseOrderByStartDateDescNameAsc();

	Optional<AcademicYear> findFirstByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndDeletedFalseOrderByStartDateDescNameAsc(
			java.time.LocalDate startDate,
			java.time.LocalDate endDate);

	Optional<AcademicYear> findFirstByActiveTrueAndDeletedFalseOrderByStartDateDescNameAsc();

	@Modifying(flushAutomatically = true)
	@Query("""
			update AcademicYear year
			set year.currentYear = false
			where year.deleted = false
			  and year.currentYear = true
			  and (:excludedId is null or year.id <> :excludedId)
			""")
	int clearCurrentYearExcept(@Param("excludedId") UUID excludedId);

	@Query("""
			select count(year) > 0
			from AcademicYear year
			where year.deleted = false
			  and year.active = true
			  and (:excludedId is null or year.id <> :excludedId)
			  and year.startDate <= :endDate
			  and year.endDate >= :startDate
			""")
	boolean existsOverlappingActiveYear(
			@Param("startDate") java.time.LocalDate startDate,
			@Param("endDate") java.time.LocalDate endDate,
			@Param("excludedId") UUID excludedId);
}
