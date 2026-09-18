package com.school.erp.modules.staff.infrastructure;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.LeaveStatus;
import com.school.erp.modules.staff.domain.StaffLeaveRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffLeaveRequestRepository extends BaseRepository<StaffLeaveRequest, UUID> {

	@Query("""
			select count(request) > 0
			from StaffLeaveRequest request
			where request.deleted = false
			  and request.staff.id = :staffId
			  and request.status in :statuses
			  and (:excludedId is null or request.id <> :excludedId)
			  and request.startDate <= :endDate
			  and request.endDate >= :startDate
			""")
	boolean existsOverlap(
			@Param("staffId") UUID staffId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("statuses") Collection<LeaveStatus> statuses,
			@Param("excludedId") UUID excludedId);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation", "leaveType" })
	@Query("""
			select request
			from StaffLeaveRequest request
			where request.deleted = false
			  and request.status = com.school.erp.modules.staff.domain.LeaveStatus.APPROVED
			  and request.staff.id in :staffIds
			  and request.startDate <= :attendanceDate
			  and request.endDate >= :attendanceDate
			""")
	List<StaffLeaveRequest> findApprovedCoveringDate(
			@Param("staffIds") Collection<UUID> staffIds,
			@Param("attendanceDate") LocalDate attendanceDate);

	@Query("""
			select count(request) > 0
			from StaffLeaveRequest request
			where request.deleted = false
			  and request.status = com.school.erp.modules.staff.domain.LeaveStatus.APPROVED
			  and request.staff.id = :staffId
			  and request.startDate <= :attendanceDate
			  and request.endDate >= :attendanceDate
			""")
	boolean existsApprovedForStaffAndDate(
			@Param("staffId") UUID staffId,
			@Param("attendanceDate") LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation", "leaveType" })
	@Query("""
			select request
			from StaffLeaveRequest request
			where request.deleted = false
			  and (:staffId is null or request.staff.id = :staffId)
			  and (:status is null or request.status = :status)
			  and (:fromDate is null or request.endDate >= :fromDate)
			  and (:toDate is null or request.startDate <= :toDate)
			""")
	Page<StaffLeaveRequest> search(
			@Param("staffId") UUID staffId,
			@Param("status") LeaveStatus status,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			Pageable pageable);

	@EntityGraph(attributePaths = { "staff", "staff.department", "staff.designation", "leaveType" })
	@Query("""
			select request
			from StaffLeaveRequest request
			where request.deleted = false
			  and (:staffId is null or request.staff.id = :staffId)
			  and (:status is null or request.status = :status)
			  and (:fromDate is null or request.endDate >= :fromDate)
			  and (:toDate is null or request.startDate <= :toDate)
			order by request.startDate desc, request.createdAt desc
			""")
	List<StaffLeaveRequest> findReportRows(
			@Param("staffId") UUID staffId,
			@Param("status") LeaveStatus status,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);
}
