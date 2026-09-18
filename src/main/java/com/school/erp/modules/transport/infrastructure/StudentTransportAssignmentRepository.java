package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentTransportAssignmentRepository extends BaseRepository<StudentTransportAssignment, UUID> {

	boolean existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
			UUID studentId,
			UUID academicYearId,
			TransportStatus status);

	long countByVehicleIdAndAcademicYearIdAndStatusAndDeletedFalse(
			UUID vehicleId,
			UUID academicYearId,
			TransportStatus status);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"vehicle",
			"vehicle.driver",
			"route",
			"pickupPoint"
	})
	Optional<StudentTransportAssignment> findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
			UUID studentId,
			UUID academicYearId,
			TransportStatus status);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"vehicle",
			"vehicle.driver",
			"route",
			"pickupPoint"
	})
	Optional<StudentTransportAssignment> findFirstByStudentIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
			UUID studentId,
			TransportStatus status);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"vehicle",
			"vehicle.driver",
			"route",
			"pickupPoint"
	})
	List<StudentTransportAssignment> findByVehicleIdAndAcademicYearIdAndDeletedFalseOrderByAssignmentDateAsc(
			UUID vehicleId,
			UUID academicYearId);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"vehicle",
			"vehicle.driver",
			"route",
			"pickupPoint"
	})
	Optional<StudentTransportAssignment> findDetailedByIdAndDeletedFalse(UUID id);

	@EntityGraph(attributePaths = {
			"student",
			"student.classAssignments",
			"academicYear",
			"vehicle",
			"vehicle.driver",
			"route",
			"pickupPoint"
	})
	@Query("""
			select assignment
			from StudentTransportAssignment assignment
			where assignment.deleted = false
			  and (:academicYearId is null or assignment.academicYear.id = :academicYearId)
			  and (:vehicleId is null or assignment.vehicle.id = :vehicleId)
			  and (:routeId is null or assignment.route.id = :routeId)
			  and (:studentId is null or assignment.student.id = :studentId)
			  and (:status is null or assignment.status = :status)
			order by assignment.academicYear.startDate desc, assignment.vehicle.vehicleNumber asc, assignment.route.routeName asc, assignment.assignmentDate asc
			""")
	List<StudentTransportAssignment> findReportAssignments(
			@Param("academicYearId") UUID academicYearId,
			@Param("vehicleId") UUID vehicleId,
			@Param("routeId") UUID routeId,
			@Param("studentId") UUID studentId,
			@Param("status") TransportStatus status);
}
