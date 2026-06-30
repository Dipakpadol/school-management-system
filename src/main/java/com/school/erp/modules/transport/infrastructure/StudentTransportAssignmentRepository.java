package com.school.erp.modules.transport.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportStatus;

import org.springframework.data.jpa.repository.EntityGraph;

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
}
