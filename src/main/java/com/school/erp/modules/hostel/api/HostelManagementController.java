package com.school.erp.modules.hostel.api;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.fees.domain.FeeStructureStatus;
import com.school.erp.modules.academic.api.dto.AcademicYearResponse;
import com.school.erp.modules.hostel.api.dto.ChangeRoomRequest;
import com.school.erp.modules.hostel.api.dto.HostelAllocationResponse;
import com.school.erp.modules.hostel.api.dto.HostelFeeAssignmentRequest;
import com.school.erp.modules.hostel.api.dto.HostelFeeAssignmentResponse;
import com.school.erp.modules.hostel.api.dto.HostelFeeStructureRequest;
import com.school.erp.modules.hostel.api.dto.HostelFeeStructureResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomDetailsResponse;
import com.school.erp.modules.hostel.api.dto.HostelRoomSummaryResponse;
import com.school.erp.modules.hostel.api.dto.HostelStudentRoomResponse;
import com.school.erp.modules.hostel.api.dto.HostelSummaryResponse;
import com.school.erp.modules.hostel.api.dto.RoomStudentAssignmentRequest;
import com.school.erp.modules.hostel.api.dto.VacateHostelRequest;
import com.school.erp.modules.hostel.application.HostelService;

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
@RequestMapping("/v1/hostels")
@RequiredArgsConstructor
@Tag(name = "Hostel Management", description = "Typed hostel room, allocation, and hostel fee management APIs.")
public class HostelManagementController {

	private final HostelService hostelService;

	@GetMapping("/academic-years")
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "List academic years for hostel management")
	public ResponseEntity<ApiResponse<List<AcademicYearResponse>>> academicYears(HttpServletRequest request) {
		return ok(hostelService.academicYears(), "Academic years fetched successfully", request);
	}

	@GetMapping
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "List hostels")
	public ResponseEntity<ApiResponse<List<HostelSummaryResponse>>> hostels(HttpServletRequest request) {
		return ok(hostelService.hostels(), "Hostels fetched successfully", request);
	}

	@GetMapping("/academic-years/{academicYearId}/rooms")
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "List hostel rooms with occupancy for an academic year")
	public ResponseEntity<ApiResponse<List<HostelRoomSummaryResponse>>> rooms(
			@PathVariable UUID academicYearId,
			HttpServletRequest request) {
		return ok(hostelService.roomsForAcademicYear(academicYearId), "Hostel rooms fetched successfully", request);
	}

	@GetMapping("/rooms/{roomId}/details")
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "Get room details and assigned students")
	public ResponseEntity<ApiResponse<HostelRoomDetailsResponse>> roomDetails(
			@PathVariable UUID roomId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(hostelService.roomDetails(roomId, academicYearId), "Room details fetched successfully", request);
	}

	@GetMapping("/rooms/{roomId}/students")
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "Get students assigned to a room")
	public ResponseEntity<ApiResponse<List<HostelStudentRoomResponse>>> roomStudents(
			@PathVariable UUID roomId,
			@RequestParam UUID academicYearId,
			HttpServletRequest request) {
		return ok(hostelService.studentsInRoom(roomId, academicYearId), "Room students fetched successfully", request);
	}

	@PostMapping("/rooms/{roomId}/assign-student")
	@PreAuthorize("hasAuthority('HOSTEL_MANAGE')")
	@Operation(summary = "Assign a student to a hostel room")
	public ResponseEntity<ApiResponse<HostelAllocationResponse>> assignStudent(
			@PathVariable UUID roomId,
			@Valid @RequestBody RoomStudentAssignmentRequest body,
			HttpServletRequest request) {
		return created(hostelService.assignStudentToRoom(roomId, body), "Student assigned to room successfully", request);
	}

	@PutMapping("/allocations/{allocationId}/change-room")
	@PreAuthorize("hasAuthority('HOSTEL_MANAGE')")
	@Operation(summary = "Change hostel room for an allocation")
	public ResponseEntity<ApiResponse<HostelAllocationResponse>> changeRoom(
			@PathVariable UUID allocationId,
			@Valid @RequestBody ChangeRoomRequest body,
			HttpServletRequest request) {
		return ok(hostelService.changeRoom(allocationId, body), "Hostel room changed successfully", request);
	}

	@PutMapping("/allocations/{allocationId}/vacate")
	@PreAuthorize("hasAuthority('HOSTEL_MANAGE')")
	@Operation(summary = "Vacate a hostel allocation")
	public ResponseEntity<ApiResponse<HostelAllocationResponse>> vacate(
			@PathVariable UUID allocationId,
			@Valid @RequestBody VacateHostelRequest body,
			HttpServletRequest request) {
		return ok(hostelService.vacate(allocationId, body), "Hostel vacated successfully", request);
	}

	@GetMapping("/students/{studentId}/allocations")
	@PreAuthorize("hasAuthority('HOSTEL_READ')")
	@Operation(summary = "Get hostel allocations for a student")
	public ResponseEntity<ApiResponse<List<HostelAllocationResponse>>> studentAllocations(
			@PathVariable UUID studentId,
			HttpServletRequest request) {
		return ok(hostelService.studentAllocations(studentId), "Student hostel allocations fetched successfully", request);
	}

	@PostMapping("/fees/structures")
	@PreAuthorize("hasAnyAuthority('HOSTEL_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Create hostel fee structure")
	public ResponseEntity<ApiResponse<HostelFeeStructureResponse>> createFeeStructure(
			@Valid @RequestBody HostelFeeStructureRequest body,
			HttpServletRequest request) {
		return created(hostelService.createFeeStructure(body), "Hostel fee structure created successfully", request);
	}

	@PutMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('HOSTEL_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Update hostel fee structure")
	public ResponseEntity<ApiResponse<HostelFeeStructureResponse>> updateFeeStructure(
			@PathVariable UUID structureId,
			@Valid @RequestBody HostelFeeStructureRequest body,
			HttpServletRequest request) {
		return ok(hostelService.updateFeeStructure(structureId, body), "Hostel fee structure updated successfully", request);
	}

	@GetMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('HOSTEL_READ','FEES_READ')")
	@Operation(summary = "Get hostel fee structure")
	public ResponseEntity<ApiResponse<HostelFeeStructureResponse>> getFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest request) {
		return ok(hostelService.getFeeStructure(structureId), "Hostel fee structure fetched successfully", request);
	}

	@GetMapping("/fees/structures")
	@PreAuthorize("hasAnyAuthority('HOSTEL_READ','FEES_READ')")
	@Operation(summary = "List hostel fee structures")
	public ResponseEntity<ApiResponse<PageResponse<HostelFeeStructureResponse>>> listFeeStructures(
			@RequestParam(required = false) UUID academicYearId,
			@RequestParam(required = false) UUID hostelId,
			@RequestParam(required = false) String roomType,
			@RequestParam(required = false) FeeStructureStatus status,
			@Valid @ParameterObject PageRequestDto pageRequest,
			HttpServletRequest request) {
		return ok(
				hostelService.listFeeStructures(academicYearId, hostelId, roomType, status, pageRequest),
				"Hostel fee structures fetched successfully",
				request);
	}

	@DeleteMapping("/fees/structures/{structureId}")
	@PreAuthorize("hasAnyAuthority('HOSTEL_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Soft delete hostel fee structure")
	public ResponseEntity<ApiResponse<HostelFeeStructureResponse>> deleteFeeStructure(
			@PathVariable UUID structureId,
			HttpServletRequest request) {
		return ok(hostelService.deleteFeeStructure(structureId), "Hostel fee structure deleted successfully", request);
	}

	@PostMapping("/fees/assign")
	@PreAuthorize("hasAnyAuthority('HOSTEL_MANAGE','FEES_MANAGE')")
	@Operation(summary = "Assign hostel fee to student")
	public ResponseEntity<ApiResponse<HostelFeeAssignmentResponse>> assignHostelFee(
			@Valid @RequestBody HostelFeeAssignmentRequest body,
			HttpServletRequest request) {
		return created(hostelService.assignHostelFees(body), "Hostel fee assigned successfully", request);
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
