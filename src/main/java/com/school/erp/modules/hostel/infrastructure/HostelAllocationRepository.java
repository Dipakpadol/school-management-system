package com.school.erp.modules.hostel.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.hostel.domain.HostelAllocation;
import com.school.erp.modules.hostel.domain.HostelAllocationStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HostelAllocationRepository extends BaseRepository<HostelAllocation, UUID> {

	boolean existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
			UUID studentId,
			UUID academicYearId,
			HostelAllocationStatus status);

	boolean existsByAcademicYearIdAndBedIdAndStatusAndDeletedFalse(
			UUID academicYearId,
			UUID bedId,
			HostelAllocationStatus status);

	boolean existsByHostelIdAndStatusAndDeletedFalse(UUID hostelId, HostelAllocationStatus status);

	boolean existsByRoomIdAndStatusAndDeletedFalse(UUID roomId, HostelAllocationStatus status);

	boolean existsByBedIdAndStatusAndDeletedFalse(UUID bedId, HostelAllocationStatus status);

	long countByAcademicYearIdAndRoomIdAndStatusAndDeletedFalse(
			UUID academicYearId,
			UUID roomId,
			HostelAllocationStatus status);

	@Query("""
			select count(allocation.id)
			from HostelAllocation allocation
			where allocation.room.id = :roomId
			  and allocation.status = :status
			  and allocation.deleted = false
			group by allocation.academicYear.id
			""")
	List<Long> activeOccupancyCountsByRoom(
			@Param("roomId") UUID roomId,
			@Param("status") HostelAllocationStatus status);

	@Query("""
			select count(allocation.id) > 0
			from HostelAllocation allocation
			where allocation.room.id = :roomId
			  and allocation.bed is not null
			  and allocation.status = :status
			  and allocation.deleted = false
			""")
	boolean existsActiveBedAllocation(
			@Param("roomId") UUID roomId,
			@Param("status") HostelAllocationStatus status);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"hostel",
			"room",
			"bed"
	})
	@Query("select allocation from HostelAllocation allocation where allocation.id = :id and allocation.deleted = false")
	Optional<HostelAllocation> findDetailedByIdAndDeletedFalse(@Param("id") UUID id);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"hostel",
			"room",
			"bed"
	})
	List<HostelAllocation> findByRoomIdAndAcademicYearIdAndDeletedFalseOrderByAllocationDateAsc(
			UUID roomId,
			UUID academicYearId);

	@EntityGraph(attributePaths = { "academicYear", "hostel", "room", "bed" })
	List<HostelAllocation> findByStudentIdAndDeletedFalseOrderByAllocationDateDesc(UUID studentId);

	@EntityGraph(attributePaths = { "student", "academicYear", "hostel", "room", "bed" })
	Optional<HostelAllocation> findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
			UUID studentId,
			UUID academicYearId,
			HostelAllocationStatus status);

	@EntityGraph(attributePaths = { "student", "academicYear", "hostel", "room", "bed" })
	Optional<HostelAllocation> findFirstByStudentIdAndStatusAndDeletedFalseOrderByAllocationDateDesc(
			UUID studentId,
			HostelAllocationStatus status);
}
