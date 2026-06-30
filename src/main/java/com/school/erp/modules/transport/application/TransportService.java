package com.school.erp.modules.transport.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.fees.api.dto.StudentFeeAssignmentResponse;
import com.school.erp.modules.fees.application.FeeService;
import com.school.erp.modules.fees.domain.FeeAssignmentStatus;
import com.school.erp.modules.fees.domain.FeeScope;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.fees.infrastructure.StudentFeeAssignmentRepository;
import com.school.erp.modules.students.domain.Student;
import com.school.erp.modules.students.infrastructure.StudentRepository;
import com.school.erp.modules.transport.api.dto.StudentTransportAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportAssignmentRequest;
import com.school.erp.modules.transport.api.dto.TransportDriverRequest;
import com.school.erp.modules.transport.api.dto.TransportDriverResponse;
import com.school.erp.modules.transport.api.dto.TransportPickupPointRequest;
import com.school.erp.modules.transport.api.dto.TransportPickupPointResponse;
import com.school.erp.modules.transport.api.dto.TransportRemoveRequest;
import com.school.erp.modules.transport.api.dto.TransportRouteRequest;
import com.school.erp.modules.transport.api.dto.TransportRouteResponse;
import com.school.erp.modules.transport.api.dto.TransportStudentAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleDetailsResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleRequest;
import com.school.erp.modules.transport.api.dto.TransportVehicleResponse;
import com.school.erp.modules.transport.domain.StudentTransportAssignment;
import com.school.erp.modules.transport.domain.TransportDriver;
import com.school.erp.modules.transport.domain.TransportFeeStructure;
import com.school.erp.modules.transport.domain.TransportPickupPoint;
import com.school.erp.modules.transport.domain.TransportRoute;
import com.school.erp.modules.transport.domain.TransportStatus;
import com.school.erp.modules.transport.domain.TransportVehicle;
import com.school.erp.modules.transport.infrastructure.StudentTransportAssignmentRepository;
import com.school.erp.modules.transport.infrastructure.TransportDriverRepository;
import com.school.erp.modules.transport.infrastructure.TransportFeeStructureRepository;
import com.school.erp.modules.transport.infrastructure.TransportPickupPointRepository;
import com.school.erp.modules.transport.infrastructure.TransportRouteRepository;
import com.school.erp.modules.transport.infrastructure.TransportVehicleRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransportService {

	private static final String MODULE_NAME = "TRANSPORT";

	private final TransportDriverRepository driverRepository;
	private final TransportVehicleRepository vehicleRepository;
	private final TransportRouteRepository routeRepository;
	private final TransportPickupPointRepository pickupPointRepository;
	private final StudentTransportAssignmentRepository assignmentRepository;
	private final TransportFeeStructureRepository transportFeeStructureRepository;
	private final StudentFeeAssignmentRepository studentFeeAssignmentRepository;
	private final StudentRepository studentRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final FeeService feeService;
	private final TransportMapper transportMapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public List<AcademicYearResponse> academicYears() {
		return academicHierarchyService.getAcademicYears();
	}

	@Transactional(readOnly = true)
	public List<TransportDriverResponse> drivers() {
		return driverRepository.findAllByDeletedFalseOrderByFirstNameAscLastNameAsc().stream()
				.map(transportMapper::toDriverResponse)
				.toList();
	}

	@Transactional
	public TransportDriverResponse createDriver(TransportDriverRequest request) {
		validateActiveInactiveStatus(request.status(), "Driver");
		driverRepository.findByLicenseNumberIgnoreCaseAndDeletedFalse(request.licenseNumber())
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Driver license number already exists.");
				});
		TransportDriver driver = new TransportDriver(
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.mobileNumber(),
				request.licenseNumber(),
				request.licenseExpiryDate(),
				request.address(),
				statusOrActive(request.status()));
		TransportDriverResponse response = transportMapper.toDriverResponse(driverRepository.save(driver));
		audit("TransportDriver", response.id(), "DRIVER_CREATED", null, response);
		return response;
	}

	@Transactional
	public TransportDriverResponse updateDriver(UUID driverId, TransportDriverRequest request) {
		validateActiveInactiveStatus(request.status(), "Driver");
		TransportDriver driver = loadDriver(driverId);
		TransportDriverResponse oldValue = transportMapper.toDriverResponse(driver);
		driverRepository.findByLicenseNumberIgnoreCaseAndDeletedFalse(request.licenseNumber())
				.filter(existing -> !existing.getId().equals(driverId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Driver license number already exists.");
				});
		driver.update(
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.mobileNumber(),
				request.licenseNumber(),
				request.licenseExpiryDate(),
				request.address(),
				statusOrActive(request.status()));
		TransportDriverResponse response = transportMapper.toDriverResponse(driver);
		audit("TransportDriver", driverId, "DRIVER_UPDATED", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public TransportDriverResponse getDriver(UUID driverId) {
		return transportMapper.toDriverResponse(loadDriver(driverId));
	}

	@Transactional
	public TransportDriverResponse deleteDriver(UUID driverId) {
		TransportDriver driver = loadDriver(driverId);
		TransportDriverResponse oldValue = transportMapper.toDriverResponse(driver);
		driver.softDelete(currentActor());
		audit("TransportDriver", driverId, "DRIVER_DELETED", oldValue, Map.of("deleted", true, "driverId", driverId));
		return transportMapper.toDriverResponse(driver);
	}

	@Transactional(readOnly = true)
	public List<TransportVehicleResponse> vehicles(UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		return vehicleRepository.findByAcademicYearIdAndDeletedFalseOrderByVehicleNumberAsc(academicYearId).stream()
				.map(vehicle -> transportMapper.toVehicleResponse(vehicle, occupiedCount(academicYearId, vehicle.getId())))
				.toList();
	}

	@Transactional
	public TransportVehicleResponse createVehicle(TransportVehicleRequest request) {
		validateActiveInactiveStatus(request.status(), "Vehicle");
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		validateVehicleNumberAvailable(request.vehicleNumber(), academicYear.getId(), null);
		TransportDriver driver = request.driverId() == null ? null : loadDriver(request.driverId());
		TransportVehicle vehicle = new TransportVehicle(
				academicYear,
				request.vehicleNumber(),
				request.vehicleName(),
				request.vehicleType(),
				request.capacity(),
				driver,
				statusOrActive(request.status()));
		TransportVehicle saved = vehicleRepository.save(vehicle);
		TransportVehicleResponse response = transportMapper.toVehicleResponse(saved, 0);
		audit("TransportVehicle", response.id(), "VEHICLE_CREATED", null, response);
		return response;
	}

	@Transactional
	public TransportVehicleResponse updateVehicle(UUID vehicleId, TransportVehicleRequest request) {
		validateActiveInactiveStatus(request.status(), "Vehicle");
		TransportVehicle vehicle = loadVehicle(vehicleId);
		TransportVehicleResponse oldValue = transportMapper.toVehicleResponse(
				vehicle,
				occupiedCount(vehicle.getAcademicYear().getId(), vehicleId));
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		validateVehicleNumberAvailable(request.vehicleNumber(), academicYear.getId(), vehicleId);
		TransportDriver driver = request.driverId() == null ? null : loadDriver(request.driverId());
		long occupied = assignmentRepository.countByVehicleIdAndAcademicYearIdAndStatusAndDeletedFalse(
				vehicleId,
				academicYear.getId(),
				TransportStatus.ASSIGNED);
		if (occupied > request.capacity()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle capacity cannot be lower than occupied seats.");
		}
		vehicle.update(
				academicYear,
				request.vehicleNumber(),
				request.vehicleName(),
				request.vehicleType(),
				request.capacity(),
				driver,
				statusOrActive(request.status()));
		TransportVehicleResponse response = transportMapper.toVehicleResponse(vehicle, (int) occupied);
		audit("TransportVehicle", vehicleId, "VEHICLE_UPDATED", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public TransportVehicleResponse getVehicle(UUID vehicleId) {
		TransportVehicle vehicle = loadVehicle(vehicleId);
		return transportMapper.toVehicleResponse(vehicle, occupiedCount(vehicle.getAcademicYear().getId(), vehicleId));
	}

	@Transactional
	public TransportVehicleResponse deleteVehicle(UUID vehicleId) {
		TransportVehicle vehicle = loadVehicle(vehicleId);
		TransportVehicleResponse oldValue = transportMapper.toVehicleResponse(
				vehicle,
				occupiedCount(vehicle.getAcademicYear().getId(), vehicleId));
		if (occupiedCount(vehicle.getAcademicYear().getId(), vehicleId) > 0) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle has assigned students.");
		}
		vehicle.softDelete(currentActor());
		audit("TransportVehicle", vehicleId, "VEHICLE_DELETED", oldValue, Map.of("deleted", true, "vehicleId", vehicleId));
		return transportMapper.toVehicleResponse(vehicle, 0);
	}

	@Transactional(readOnly = true)
	public List<TransportRouteResponse> routes(UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		return routeRepository.findByAcademicYearIdAndDeletedFalseOrderByRouteNameAsc(academicYearId).stream()
				.map(transportMapper::toRouteResponse)
				.toList();
	}

	@Transactional
	public TransportRouteResponse createRoute(TransportRouteRequest request) {
		validateActiveInactiveStatus(request.status(), "Route");
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		validateRouteCodeAvailable(request.routeCode(), academicYear.getId(), null);
		TransportVehicle vehicle = request.vehicleId() == null ? null : loadVehicle(request.vehicleId());
		ensureVehicleAcademicYear(vehicle, academicYear);
		TransportRoute route = new TransportRoute(
				academicYear,
				request.routeName(),
				request.routeCode(),
				request.startLocation(),
				request.endLocation(),
				vehicle,
				statusOrActive(request.status()));
		TransportRouteResponse response = transportMapper.toRouteResponse(routeRepository.save(route));
		audit("TransportRoute", response.id(), "ROUTE_CREATED", null, response);
		return response;
	}

	@Transactional
	public TransportRouteResponse updateRoute(UUID routeId, TransportRouteRequest request) {
		validateActiveInactiveStatus(request.status(), "Route");
		TransportRoute route = loadRoute(routeId);
		TransportRouteResponse oldValue = transportMapper.toRouteResponse(route);
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(request.academicYearId());
		validateRouteCodeAvailable(request.routeCode(), academicYear.getId(), routeId);
		TransportVehicle vehicle = request.vehicleId() == null ? null : loadVehicle(request.vehicleId());
		ensureVehicleAcademicYear(vehicle, academicYear);
		route.update(
				academicYear,
				request.routeName(),
				request.routeCode(),
				request.startLocation(),
				request.endLocation(),
				vehicle,
				statusOrActive(request.status()));
		TransportRouteResponse response = transportMapper.toRouteResponse(route);
		audit("TransportRoute", routeId, "ROUTE_UPDATED", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public TransportRouteResponse getRoute(UUID routeId) {
		return transportMapper.toRouteResponse(loadRoute(routeId));
	}

	@Transactional
	public TransportRouteResponse deleteRoute(UUID routeId) {
		TransportRoute route = loadRoute(routeId);
		TransportRouteResponse oldValue = transportMapper.toRouteResponse(route);
		route.softDelete(currentActor());
		audit("TransportRoute", routeId, "ROUTE_DELETED", oldValue, Map.of("deleted", true, "routeId", routeId));
		return transportMapper.toRouteResponse(route);
	}

	@Transactional(readOnly = true)
	public List<TransportPickupPointResponse> pickupPoints(UUID routeId) {
		loadRoute(routeId);
		return pickupPointRepository.findByRouteIdAndDeletedFalseOrderBySequenceOrderAscPointNameAsc(routeId).stream()
				.map(transportMapper::toPickupPointResponse)
				.toList();
	}

	@Transactional
	public TransportPickupPointResponse createPickupPoint(UUID routeId, TransportPickupPointRequest request) {
		validateActiveInactiveStatus(request.status(), "Pickup point");
		TransportRoute route = loadRoute(routeId);
		TransportPickupPoint point = new TransportPickupPoint(
				route,
				request.pointName(),
				request.pickupTime(),
				request.dropTime(),
				request.monthlyFee(),
				request.sequenceOrder(),
				statusOrActive(request.status()));
		TransportPickupPointResponse response = transportMapper.toPickupPointResponse(pickupPointRepository.save(point));
		audit("TransportPickupPoint", response.id(), "PICKUP_POINT_CREATED", null, response);
		return response;
	}

	@Transactional
	public TransportPickupPointResponse updatePickupPoint(UUID pickupPointId, TransportPickupPointRequest request) {
		validateActiveInactiveStatus(request.status(), "Pickup point");
		TransportPickupPoint point = loadPickupPoint(pickupPointId);
		TransportPickupPointResponse oldValue = transportMapper.toPickupPointResponse(point);
		point.update(
				point.getRoute(),
				request.pointName(),
				request.pickupTime(),
				request.dropTime(),
				request.monthlyFee(),
				request.sequenceOrder(),
				statusOrActive(request.status()));
		TransportPickupPointResponse response = transportMapper.toPickupPointResponse(point);
		audit("TransportPickupPoint", pickupPointId, "PICKUP_POINT_UPDATED", oldValue, response);
		return response;
	}

	@Transactional
	public TransportPickupPointResponse deletePickupPoint(UUID pickupPointId) {
		TransportPickupPoint point = loadPickupPoint(pickupPointId);
		TransportPickupPointResponse oldValue = transportMapper.toPickupPointResponse(point);
		point.softDelete(currentActor());
		audit("TransportPickupPoint", pickupPointId, "PICKUP_POINT_DELETED", oldValue, Map.of("deleted", true, "pickupPointId", pickupPointId));
		return transportMapper.toPickupPointResponse(point);
	}

	@Transactional(readOnly = true)
	public TransportVehicleDetailsResponse vehicleDetails(UUID vehicleId, UUID academicYearId) {
		AcademicYear academicYear = academicHierarchyService.loadAcademicYear(academicYearId);
		TransportVehicle vehicle = loadVehicle(vehicleId);
		if (!vehicle.getAcademicYear().getId().equals(academicYear.getId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle does not belong to the selected academic year.");
		}
		List<TransportRoute> routes = routeRepository
				.findByVehicleIdAndAcademicYearIdAndDeletedFalseOrderByRouteNameAsc(vehicleId, academicYearId);
		List<TransportPickupPointResponse> pickupPoints = routes.stream()
				.flatMap(route -> pickupPointRepository
						.findByRouteIdAndDeletedFalseOrderBySequenceOrderAscPointNameAsc(route.getId()).stream())
				.map(transportMapper::toPickupPointResponse)
				.toList();
		List<TransportStudentAssignmentResponse> students = studentsByVehicle(vehicleId, academicYearId);
		return transportMapper.toVehicleDetails(
				vehicle,
				occupiedCount(academicYearId, vehicleId),
				routes.stream().map(transportMapper::toRouteResponse).toList(),
				pickupPoints,
				students);
	}

	@Transactional(readOnly = true)
	public List<TransportStudentAssignmentResponse> studentsByVehicle(UUID vehicleId, UUID academicYearId) {
		academicHierarchyService.loadAcademicYear(academicYearId);
		loadVehicle(vehicleId);
		return assignmentRepository.findByVehicleIdAndAcademicYearIdAndDeletedFalseOrderByAssignmentDateAsc(vehicleId, academicYearId)
				.stream()
				.filter(StudentTransportAssignment::isAssigned)
				.map(transportMapper::toStudentAssignmentSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public Optional<StudentTransportAssignmentResponse> currentStudentAssignment(UUID studentId, UUID academicYearId) {
		studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
		if (academicYearId != null) {
			academicHierarchyService.loadAcademicYear(academicYearId);
			return assignmentRepository
					.findFirstByStudentIdAndAcademicYearIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(
							studentId,
							academicYearId,
							TransportStatus.ASSIGNED)
					.map(transportMapper::toAssignmentResponse);
		}
		return assignmentRepository
				.findFirstByStudentIdAndStatusAndDeletedFalseOrderByAssignmentDateDesc(studentId, TransportStatus.ASSIGNED)
				.map(transportMapper::toAssignmentResponse);
	}

	@Transactional
	public Optional<StudentTransportAssignmentResponse> assignStudentDuringAdmission(
			Student student,
			AcademicYear fallbackAcademicYear,
			TransportAssignmentRequest request) {
		if (!transportRequested(request)) {
			return Optional.empty();
		}
		ResolvedTransportAssignment resolved = resolveAssignmentRequest(fallbackAcademicYear, request);
		StudentTransportAssignment assignment = createActiveAssignment(
				student,
				resolved.academicYear(),
				resolved.vehicle(),
				resolved.route(),
				resolved.pickupPoint(),
				defaultDate(request.assignmentDate(), student.getAdmissionDate()));
		if (request.appliesTransportFee()) {
			List<StudentFeeAssignmentResponse> fees = assignApplicableTransportFees(assignment);
			assignment.markFeeAssigned(!fees.isEmpty());
		}
		StudentTransportAssignmentResponse response = transportMapper.toAssignmentResponse(assignment);
		audit("StudentTransportAssignment", response.id(), "TRANSPORT_ASSIGNED", null, response);
		return Optional.of(response);
	}

	@Transactional
	public Optional<StudentTransportAssignmentResponse> assignStudentByRequest(
			UUID studentId,
			UUID fallbackAcademicYearId,
			TransportAssignmentRequest request) {
		if (!transportRequested(request)) {
			return Optional.empty();
		}
		Student student = loadStudent(studentId);
		AcademicYear fallbackAcademicYear = fallbackAcademicYearId == null
				? null
				: academicHierarchyService.loadAcademicYear(fallbackAcademicYearId);
		return assignStudentDuringAdmission(student, fallbackAcademicYear, request);
	}

	@Transactional
	public StudentTransportAssignmentResponse assignStudentFromProfile(UUID studentId, TransportAssignmentRequest request) {
		return assignStudentByRequest(studentId, null, request)
				.orElseThrow(() -> new BusinessException(
						ErrorCode.VALIDATION_ERROR,
						"Academic year, route, and pickup point are required for transport assignment."));
	}

	@Transactional
	public StudentTransportAssignmentResponse changeStudentAssignment(
			UUID studentId,
			UUID assignmentId,
			TransportAssignmentRequest request) {
		StudentTransportAssignment current = loadAssignment(assignmentId);
		ensureAssignmentBelongsToStudent(current, studentId);
		if (!current.isAssigned()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active transport assignments can be changed.");
		}
		StudentTransportAssignmentResponse oldValue = transportMapper.toAssignmentResponse(current);
		ResolvedTransportAssignment resolved = resolveAssignmentRequest(current.getAcademicYear(), request);
		validateVehicleAvailable(resolved.academicYear(), resolved.vehicle());
		current.transfer(defaultDate(request.assignmentDate(), LocalDate.now()));
		StudentTransportAssignment replacement = assignmentRepository.save(new StudentTransportAssignment(
				current.getStudent(),
				resolved.academicYear(),
				resolved.vehicle(),
				resolved.route(),
				resolved.pickupPoint(),
				defaultDate(request.assignmentDate(), LocalDate.now())));
		if (request.appliesTransportFee()) {
			List<StudentFeeAssignmentResponse> fees = assignApplicableTransportFees(replacement);
			replacement.markFeeAssigned(!fees.isEmpty());
		}
		StudentTransportAssignmentResponse response = transportMapper.toAssignmentResponse(replacement);
		audit("StudentTransportAssignment", assignmentId, "TRANSPORT_CHANGED", oldValue, response);
		return response;
	}

	@Transactional
	public StudentTransportAssignmentResponse removeStudentAssignment(
			UUID studentId,
			UUID assignmentId,
			TransportRemoveRequest request) {
		StudentTransportAssignment assignment = loadAssignment(assignmentId);
		ensureAssignmentBelongsToStudent(assignment, studentId);
		if (!assignment.isAssigned()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only active transport assignments can be removed.");
		}
		StudentTransportAssignmentResponse oldValue = transportMapper.toAssignmentResponse(assignment);
		assignment.remove(defaultDate(request == null ? null : request.endDate(), LocalDate.now()));
		StudentTransportAssignmentResponse response = transportMapper.toAssignmentResponse(assignment);
		audit("StudentTransportAssignment", assignmentId, "TRANSPORT_REMOVED", oldValue, response);
		return response;
	}

	private StudentTransportAssignment createActiveAssignment(
			Student student,
			AcademicYear academicYear,
			TransportVehicle vehicle,
			TransportRoute route,
			TransportPickupPoint pickupPoint,
			LocalDate assignmentDate) {
		validateTransportActive(vehicle, route, pickupPoint);
		validateVehicleAvailable(academicYear, vehicle);
		if (assignmentRepository.existsByStudentIdAndAcademicYearIdAndStatusAndDeletedFalse(
				student.getId(),
				academicYear.getId(),
				TransportStatus.ASSIGNED)) {
			throw new BusinessException(
					ErrorCode.CONFLICT,
					"Student already has an active transport assignment for this academic year.");
		}
		return assignmentRepository.save(new StudentTransportAssignment(
				student,
				academicYear,
				vehicle,
				route,
				pickupPoint,
				assignmentDate));
	}

	private List<StudentFeeAssignmentResponse> assignApplicableTransportFees(StudentTransportAssignment assignment) {
		List<UUID> feeStructureIds = transportFeeStructureRepository.findApplicable(
				assignment.getAcademicYear().getId(),
				assignment.getRoute().getId(),
				assignment.getPickupPoint().getId(),
				FeeStructureStatus.ACTIVE).stream()
				.map(TransportFeeStructure::getBackingFeeStructure)
				.map(structure -> structure.getId())
				.toList();
		if (feeStructureIds.isEmpty()) {
			return List.of();
		}
		return feeService.assignActiveTransportFeesToStudent(
				assignment.getStudent().getId(),
				feeStructureIds,
				assignment.getAssignmentDate());
	}

	private ResolvedTransportAssignment resolveAssignmentRequest(
			AcademicYear fallbackAcademicYear,
			TransportAssignmentRequest request) {
		AcademicYear academicYear = request.academicYearId() == null
				? fallbackAcademicYear
				: academicHierarchyService.loadAcademicYear(request.academicYearId());
		if (academicYear == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Academic year is required for transport assignment.");
		}
		TransportRoute route = resolveRoute(academicYear, request);
		TransportPickupPoint pickupPoint = resolvePickupPoint(route, request);
		TransportVehicle vehicle = route.getVehicle() == null ? resolveVehicle(academicYear, request) : route.getVehicle();
		if (vehicle == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Vehicle is required when route has no mapped vehicle.");
		}
		ensureVehicleAcademicYear(vehicle, academicYear);
		if (route.getVehicle() != null && request.vehicleId() != null && !route.getVehicle().getId().equals(request.vehicleId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle is derived from the selected route.");
		}
		if (route.getVehicle() != null
				&& StringUtils.hasText(request.vehicleNumber())
				&& !route.getVehicle().getVehicleNumber().equalsIgnoreCase(request.vehicleNumber().trim())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle is derived from the selected route.");
		}
		return new ResolvedTransportAssignment(academicYear, vehicle, route, pickupPoint);
	}

	private TransportRoute resolveRoute(AcademicYear academicYear, TransportAssignmentRequest request) {
		if (request.routeId() != null) {
			TransportRoute route = loadRoute(request.routeId());
			if (!route.getAcademicYear().getId().equals(academicYear.getId())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Route does not belong to the selected academic year.");
			}
			return route;
		}
		if (!StringUtils.hasText(request.routeName())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Route is required for transport assignment.");
		}
		return routeRepository.findByRouteNameIgnoreCaseAndAcademicYearIdAndDeletedFalse(
				request.routeName(),
				academicYear.getId())
				.or(() -> routeRepository.findByRouteCodeIgnoreCaseAndAcademicYearIdAndDeletedFalse(
						request.routeName(),
						academicYear.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Transport route", request.routeName()));
	}

	private TransportPickupPoint resolvePickupPoint(TransportRoute route, TransportAssignmentRequest request) {
		if (request.pickupPointId() != null) {
			TransportPickupPoint pickupPoint = loadPickupPoint(request.pickupPointId());
			if (!pickupPoint.getRoute().getId().equals(route.getId())) {
				throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Pickup point does not belong to the selected route.");
			}
			return pickupPoint;
		}
		if (!StringUtils.hasText(request.pickupPointName())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Pickup point is required for transport assignment.");
		}
		return pickupPointRepository.findByRouteIdAndPointNameIgnoreCaseAndDeletedFalse(route.getId(), request.pickupPointName())
				.orElseThrow(() -> new ResourceNotFoundException("Transport pickup point", request.pickupPointName()));
	}

	private TransportVehicle resolveVehicle(AcademicYear academicYear, TransportAssignmentRequest request) {
		if (request.vehicleId() != null) {
			return loadVehicle(request.vehicleId());
		}
		if (StringUtils.hasText(request.vehicleNumber())) {
			return vehicleRepository
					.findByVehicleNumberIgnoreCaseAndAcademicYearIdAndDeletedFalse(request.vehicleNumber(), academicYear.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Transport vehicle", request.vehicleNumber()));
		}
		return null;
	}

	private void validateVehicleAvailable(AcademicYear academicYear, TransportVehicle vehicle) {
		long occupied = assignmentRepository.countByVehicleIdAndAcademicYearIdAndStatusAndDeletedFalse(
				vehicle.getId(),
				academicYear.getId(),
				TransportStatus.ASSIGNED);
		if (occupied >= vehicle.getCapacity()) {
			throw new BusinessException(ErrorCode.CONFLICT, "Vehicle is already full.");
		}
	}

	private void validateTransportActive(
			TransportVehicle vehicle,
			TransportRoute route,
			TransportPickupPoint pickupPoint) {
		if (!vehicle.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle is inactive.");
		}
		if (!route.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Route is inactive.");
		}
		if (!pickupPoint.isActive()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Pickup point is inactive.");
		}
	}

	private boolean transportRequested(TransportAssignmentRequest request) {
		return request != null
				&& (request.requiresTransport()
						|| request.vehicleId() != null
						|| StringUtils.hasText(request.vehicleNumber())
						|| request.routeId() != null
						|| StringUtils.hasText(request.routeName())
						|| request.pickupPointId() != null
						|| StringUtils.hasText(request.pickupPointName()));
	}

	private void validateVehicleNumberAvailable(String vehicleNumber, UUID academicYearId, UUID excludedVehicleId) {
		vehicleRepository.findByVehicleNumberIgnoreCaseAndAcademicYearIdAndDeletedFalse(vehicleNumber, academicYearId)
				.filter(existing -> excludedVehicleId == null || !existing.getId().equals(excludedVehicleId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Vehicle number already exists for this academic year.");
				});
	}

	private void validateRouteCodeAvailable(String routeCode, UUID academicYearId, UUID excludedRouteId) {
		routeRepository.findByRouteCodeIgnoreCaseAndAcademicYearIdAndDeletedFalse(routeCode, academicYearId)
				.filter(existing -> excludedRouteId == null || !existing.getId().equals(excludedRouteId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Route code already exists for this academic year.");
				});
	}

	private void ensureVehicleAcademicYear(TransportVehicle vehicle, AcademicYear academicYear) {
		if (vehicle != null && !vehicle.getAcademicYear().getId().equals(academicYear.getId())) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Vehicle does not belong to the selected academic year.");
		}
	}

	private int occupiedCount(UUID academicYearId, UUID vehicleId) {
		return (int) assignmentRepository.countByVehicleIdAndAcademicYearIdAndStatusAndDeletedFalse(
				vehicleId,
				academicYearId,
				TransportStatus.ASSIGNED);
	}

	private TransportDriver loadDriver(UUID driverId) {
		return driverRepository.findByIdAndDeletedFalse(driverId)
				.orElseThrow(() -> new ResourceNotFoundException("Transport driver", driverId));
	}

	private TransportVehicle loadVehicle(UUID vehicleId) {
		return vehicleRepository.findByIdAndDeletedFalse(vehicleId)
				.orElseThrow(() -> new ResourceNotFoundException("Transport vehicle", vehicleId));
	}

	private TransportRoute loadRoute(UUID routeId) {
		return routeRepository.findByIdAndDeletedFalse(routeId)
				.orElseThrow(() -> new ResourceNotFoundException("Transport route", routeId));
	}

	private TransportPickupPoint loadPickupPoint(UUID pickupPointId) {
		return pickupPointRepository.findByIdAndDeletedFalse(pickupPointId)
				.orElseThrow(() -> new ResourceNotFoundException("Transport pickup point", pickupPointId));
	}

	private StudentTransportAssignment loadAssignment(UUID assignmentId) {
		return assignmentRepository.findDetailedByIdAndDeletedFalse(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student transport assignment", assignmentId));
	}

	private Student loadStudent(UUID studentId) {
		return studentRepository.findByIdAndDeletedFalse(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
	}

	private void ensureAssignmentBelongsToStudent(StudentTransportAssignment assignment, UUID studentId) {
		if (!assignment.getStudent().getId().equals(studentId)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Transport assignment does not belong to the selected student.");
		}
	}

	private TransportStatus statusOrActive(TransportStatus status) {
		return status == null ? TransportStatus.ACTIVE : status;
	}

	private void validateActiveInactiveStatus(TransportStatus status, String label) {
		if (status != null && status != TransportStatus.ACTIVE && status != TransportStatus.INACTIVE) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, label + " status must be ACTIVE or INACTIVE.");
		}
	}

	private LocalDate defaultDate(LocalDate value, LocalDate fallback) {
		if (value != null) {
			return value;
		}
		return fallback == null ? LocalDate.now() : fallback;
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}

	private record ResolvedTransportAssignment(
			AcademicYear academicYear,
			TransportVehicle vehicle,
			TransportRoute route,
			TransportPickupPoint pickupPoint) {
	}
}
