package com.school.erp.modules.staff.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.StaffAttendance;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffAttendanceRepository extends BaseRepository<StaffAttendance, UUID> {

	Optional<StaffAttendance> findByStaffIdAndAttendanceDateAndDeletedFalse(UUID staffId, LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation" })
	List<StaffAttendance> findByStaffIdInAndAttendanceDateAndDeletedFalse(List<UUID> staffIds, LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation" })
	@Query("""
			select attendance
			from StaffAttendance attendance
			where attendance.deleted = false
			  and attendance.attendanceDate between :fromDate and :toDate
			  and (:departmentId is null or attendance.staff.department.id = :departmentId)
			  and (:designationId is null or attendance.staff.designation.id = :designationId)
			order by attendance.attendanceDate desc, attendance.staff.firstName asc
			""")
	List<StaffAttendance> findSummaryRecords(
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("departmentId") UUID departmentId,
			@Param("designationId") UUID designationId);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation" })
	List<StaffAttendance> findByStaffIdAndAttendanceDateBetweenAndDeletedFalseOrderByAttendanceDateDesc(
			UUID staffId,
			LocalDate fromDate,
			LocalDate toDate);
}
