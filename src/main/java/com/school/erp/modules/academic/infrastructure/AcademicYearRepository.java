package com.school.erp.modules.academic.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.academic.domain.AcademicYear;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcademicYearRepository extends BaseRepository<AcademicYear, UUID> {

	List<AcademicYear> findAllByDeletedFalseOrderByStartDateDescNameAsc();

	Optional<AcademicYear> findByCodeIgnoreCaseAndDeletedFalse(String code);

	Optional<AcademicYear> findByNameIgnoreCaseAndDeletedFalse(String name);

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
