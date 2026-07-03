package com.school.erp.modules.transport.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.transport.api.dto.TransportDriverRequest;
import com.school.erp.modules.transport.api.dto.TransportDriverResponse;
import com.school.erp.modules.transport.api.dto.TransportFeeStructureRequest;
import com.school.erp.modules.transport.api.dto.TransportFeeStructureResponse;
import com.school.erp.modules.transport.api.dto.TransportPickupPointRequest;
import com.school.erp.modules.transport.api.dto.TransportPickupPointResponse;
import com.school.erp.modules.transport.api.dto.TransportRouteRequest;
import com.school.erp.modules.transport.api.dto.TransportRouteResponse;
import com.school.erp.modules.transport.api.dto.TransportStudentAssignmentResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleDetailsResponse;
import com.school.erp.modules.transport.api.dto.TransportVehicleRequest;
import com.school.erp.modules.transport.api.dto.TransportVehicleResponse;
import com.school.erp.modules.transport.application.TransportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/v1/transport")
@RequiredArgsConstructor
@Tag(name = "Transport Management", description = "Academic year-wise vehicle, route, pickup point, driver, and student transport APIs.")
public class TransportController {

	private final TransportService transportService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List academic years for transport management")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(transportService.academicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/vehicles")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List vehicles by academic year")
	public ResponseEntity<ApiResponse<List<TransportVehicleResponse>>> vehicles(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(transportService.vehicles(academicYearId), "Vehicles fetched successfully", request);
	}

	@PostMapping("/vehicles")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Create transport vehicle")
	public ResponseEntity<ApiResponse<TransportVehicleResponse>> createVehicle(
			@Valid @RequestBody TransportVehicleRequest body,
			HttpServletRequest request) {
		return created(transportService.createVehicle(body), "Vehicle created successfully", request);
	}

	@PutMapping("/vehicles/{vehicleId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Update transport vehicle")
	public ResponseEntity<ApiResponse<TransportVehicleResponse>> updateVehicle(
			@PathVariable UUID vehicleId,
			@Valid @RequestBody TransportVehicleRequest body,
			HttpServletRequest request) {
		return ok(transportService.updateVehicle(vehicleId, body), "Vehicle updated successfully", request);
	}

	@GetMapping("/vehicles/{vehicleId}")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "Get transport vehicle")
	public ResponseEntity<ApiResponse<TransportVehicleResponse>> getVehicle(
			@PathVariable UUID vehicleId,
			HttpServletRequest request) {
		return ok(transportService.getVehicle(vehicleId), "Vehicle fetched successfully", request);
	}

	@DeleteMapping("/vehicles/{vehicleId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Soft delete transport vehicle")
	public ResponseEntity<ApiResponse<TransportVehicleResponse>> deleteVehicle(
			@PathVariable UUID vehicleId,
			HttpServletRequest request) {
		return ok(transportService.deleteVehicle(vehicleId), "Vehicle deleted successfully", request);
	}

	@GetMapping("/drivers")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List transport drivers")
	public ResponseEntity<ApiResponse<List<TransportDriverResponse>>> drivers(HttpServletRequest request) {
		return ok(transportService.drivers(), "Drivers fetched successfully", request);
	}

	@PostMapping("/drivers")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Create transport driver")
	public ResponseEntity<ApiResponse<TransportDriverResponse>> createDriver(
			@Valid @RequestBody TransportDriverRequest body,
			HttpServletRequest request) {
		return created(transportService.createDriver(body), "Driver created successfully", request);
	}

	@PutMapping("/drivers/{driverId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Update transport driver")
	public ResponseEntity<ApiResponse<TransportDriverResponse>> updateDriver(
			@PathVariable UUID driverId,
			@Valid @RequestBody TransportDriverRequest body,
			HttpServletRequest request) {
		return ok(transportService.updateDriver(driverId, body), "Driver updated successfully", request);
	}

	@GetMapping("/drivers/{driverId}")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "Get transport driver")
	public ResponseEntity<ApiResponse<TransportDriverResponse>> getDriver(
			@PathVariable UUID driverId,
			HttpServletRequest request) {
		return ok(transportService.getDriver(driverId), "Driver fetched successfully", request);
	}

	@DeleteMapping("/drivers/{driverId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Soft delete transport driver")
	public ResponseEntity<ApiResponse<TransportDriverResponse>> deleteDriver(
			@PathVariable UUID driverId,
			HttpServletRequest request) {
		return ok(transportService.deleteDriver(driverId), "Driver deleted successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/routes")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List transport routes by academic year")
	public ResponseEntity<ApiResponse<List<TransportRouteResponse>>> routes(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(transportService.routes(academicYearId), "Routes fetched successfully", request);
	}

	@PostMapping("/routes")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Create transport route")
	public ResponseEntity<ApiResponse<TransportRouteResponse>> createRoute(
			@Valid @RequestBody TransportRouteRequest body,
			HttpServletRequest request) {
		return created(transportService.createRoute(body), "Route created successfully", request);
	}

	@PutMapping("/routes/{routeId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Update transport route")
	public ResponseEntity<ApiResponse<TransportRouteResponse>> updateRoute(
			@PathVariable UUID routeId,
			@Valid @RequestBody TransportRouteRequest body,
			HttpServletRequest request) {
		return ok(transportService.updateRoute(routeId, body), "Route updated successfully", request);
	}

	@GetMapping("/routes/{routeId}")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "Get transport route")
	public ResponseEntity<ApiResponse<TransportRouteResponse>> getRoute(
			@PathVariable UUID routeId,
			HttpServletRequest request) {
		return ok(transportService.getRoute(routeId), "Route fetched successfully", request);
	}

	@DeleteMapping("/routes/{routeId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Soft delete transport route")
	public ResponseEntity<ApiResponse<TransportRouteResponse>> deleteRoute(
			@PathVariable UUID routeId,
			HttpServletRequest request) {
		return ok(transportService.deleteRoute(routeId), "Route deleted successfully", request);
	}

	@GetMapping("/routes/{routeId}/pickup-points")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List pickup points by route")
	public ResponseEntity<ApiResponse<List<TransportPickupPointResponse>>> pickupPoints(
			@PathVariable UUID routeId,
			HttpServletRequest request) {
		return ok(transportService.pickupPoints(routeId), "Pickup points fetched successfully", request);
	}

	@PostMapping("/routes/{routeId}/pickup-points")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Create pickup point")
	public ResponseEntity<ApiResponse<TransportPickupPointResponse>> createPickupPoint(
			@PathVariable UUID routeId,
			@Valid @RequestBody TransportPickupPointRequest body,
			HttpServletRequest request) {
		return created(transportService.createPickupPoint(routeId, body), "Pickup point created successfully", request);
	}

	@PutMapping("/pickup-points/{pickupPointId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Update pickup point")
	public ResponseEntity<ApiResponse<TransportPickupPointResponse>> updatePickupPoint(
			@PathVariable UUID pickupPointId,
			@Valid @RequestBody TransportPickupPointRequest body,
			HttpServletRequest request) {
		return ok(transportService.updatePickupPoint(pickupPointId, body), "Pickup point updated successfully", request);
	}

	@DeleteMapping("/pickup-points/{pickupPointId}")
	@PreAuthorize("hasAuthority('TRANSPORT_MANAGE')")
	@Operation(summary = "Soft delete pickup point")
	public ResponseEntity<ApiResponse<TransportPickupPointResponse>> deletePickupPoint(
			@PathVariable UUID pickupPointId,
			HttpServletRequest request) {
		return ok(transportService.deletePickupPoint(pickupPointId), "Pickup point deleted successfully", request);
	}

	@PostMapping("/fees/structures")
	@PreAuthorize("hasAnyAuthority('TRANSPORT_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Create transport fee structure")
	public ResponseEntity<ApiResponse<TransportFeeStructureResponse>> createFeeStructure(
			@Valid @RequestBody TransportFeeStructureRequest body,
			HttpServletRequest request) {
		return created(transportService.createFeeStructure(body), "Transport fee structure created successfully", request);
	}

	@PutMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('TRANSPORT_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Update transport fee structure")
	public ResponseEntity<ApiResponse<TransportFeeStructureResponse>> updateFeeStructure(
			@PathVariable UUID structureId,
			@Valid @RequestBody TransportFeeStructureRequest body,
			HttpServletRequest request) {
		return ok(transportService.updateFeeStructure(structureId, body), "Transport fee structure updated successfully", request);
	}

	@GetMapping("/fees/structures")
	@PreAuthorize("hasAnyAuthority('TRANSPORT_READ','FEES_READ')")
	@Operation(summary = "List transport fee structures")
	public ResponseEntity<ApiResponse<PageResponse<TransportFeeStructureResponse>>> listFeeStructures(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID routeId,
			@RequestParam(required = false) UUID pickupPointId,
			@RequestParam(required = false) FeeStructureStatus status,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				transportService.listFeeStructures(academicYearId, routeId, pickupPointId, status, pageRequest),
				"Transport fee structures fetched successfully",
				request);
	}

	@GetMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('TRANSPORT_READ','FEES_READ')")
	@Operation(summary = "Get transport fee structure")
	public ResponseEntity<ApiResponse<TransportFeeStructureResponse>> getFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest request) {
		return ok(transportService.getFeeStructure(structureId), "Transport fee structure fetched successfully", request);
	}

	@DeleteMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('TRANSPORT_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Soft delete transport fee structure")
	public ResponseEntity<ApiResponse<TransportFeeStructureResponse>> deleteFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest request) {
		return ok(transportService.deleteFeeStructure(structureId), "Transport fee structure deleted successfully", request);
	}

	@GetMapping("/vehicles/{vehicleId}/details")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "Get vehicle details with routes, pickup points, driver, and assigned students")
	public ResponseEntity<ApiResponse<TransportVehicleDetailsResponse>> vehicleDetails(
			@PathVariable UUID vehicleId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(transportService.vehicleDetails(vehicleId, academicYearId), "Vehicle details fetched successfully", request);
	}

	@GetMapping("/vehicles/{vehicleId}/students")
	@PreAuthorize("hasAuthority('TRANSPORT_READ')")
	@Operation(summary = "List students assigned to vehicle")
	public ResponseEntity<ApiResponse<List<TransportStudentAssignmentResponse>>> vehicleStudents(
			@PathVariable UUID vehicleId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(transportService.studentsByVehicle(vehicleId, academicYearId), "Vehicle students fetched successfully", request);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(response(data, message, request));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(response(data, message, request));
	}

	private <T> ApiResponse<T> response(T data, String message, HttpServletRequest request) {
		return ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID));
	}
}
