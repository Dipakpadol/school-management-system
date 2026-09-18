package com.school.erp.modules.staff.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.Staff;
import com.school.erp.modules.staff.domain.StaffType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffRepository extends BaseRepository<Staff, UUID> {

	Optional<Staff> findByEmployeeCodeIgnoreCaseAndDeletedFalse(String employeeCode);

	Optional<Staff> findByTeacherIdAndDeletedFalse(UUID teacherId);

	boolean existsByEmployeeCodeIgnoreCaseAndDeletedFalse(String employeeCode);

	long countByStatusAndDeletedFalse(EmploymentStatus status);

	long countByStaffTypeAndStatusAndDeletedFalse(StaffType staffType, EmploymentStatus status);

	@EntityGraph(attributePaths = { "department", "designation" })
	@Query("""
			select staff
			from Staff staff
			where staff.deleted = false
			  and (:status is null or staff.status = :status)
			  and (:staffType is null or staff.staffType = :staffType)
			  and (:departmentId is null or staff.department.id = :departmentId)
			  and (:designationId is null or staff.designation.id = :designationId)
			""")
	Page<Staff> search(
			@Param("status") EmploymentStatus status,
			@Param("staffType") StaffType staffType,
			@Param("departmentId") UUID departmentId,
			@Param("designationId") UUID designationId,
			Pageable pageable);

	@EntityGraph(attributePaths = { "department", "designation" })
	@Query("""
			select staff
			from Staff staff
			where staff.deleted = false
			  and staff.status = com.school.erp.modules.staff.domain.EmploymentStatus.ACTIVE
			  and staff.joiningDate <= :attendanceDate
			  and (staff.relievingDate is null or staff.relievingDate >= :attendanceDate)
			  and (:departmentId is null or staff.department.id = :departmentId)
			  and (:designationId is null or staff.designation.id = :designationId)
			order by staff.firstName asc, staff.lastName asc, staff.employeeCode asc
			""")
	List<Staff> findRoster(
			@Param("attendanceDate") LocalDate attendanceDate,
			@Param("departmentId") UUID departmentId,
			@Param("designationId") UUID designationId);
}
